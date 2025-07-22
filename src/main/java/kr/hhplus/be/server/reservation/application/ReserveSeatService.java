package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.seat.Seat;
import kr.hhplus.be.server.concert.domain.seat.SeatRepository;
import kr.hhplus.be.server.concert.domain.seat.SeatStatus;
import kr.hhplus.be.server.lock.domain.DistributedLockRepository;
import kr.hhplus.be.server.queue.domain.QueueToken;
import kr.hhplus.be.server.queue.domain.QueueTokenRepository;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.user.User;
import kr.hhplus.be.server.user.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReserveSeatService {   // 좌석 예약 서비스

    private final SeatReservationRepository reservationRepository;
    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final DistributedLockRepository distributedLockRepository;

    // 임시예약 만료 시간
    private static final int RESERVATION_TIMEOUT_MINUTES = 5;
    private static final long LOCK_TIMEOUT_MILLIS = 15000; // 락 지속 시간
    private static int MAX_RETRY = 10;   // 최대 시도 수
    private static long SLEEP_MILLIS = 200;  // 대기 시간

    /**
     * # Method설명 : 좌석 예약
     * # MethodName : reserveSeat
     **/
    public SeatReservation reserveSeat(Long userId, LocalDate concertDate, int seatNumber) {

        // 검증 단계
        User user = validateUser(userId);   // 사용자
        ConcertSchedule schedule = validateSchedule(concertDate);   // 일정
        validateQueueToken(userId, schedule.getScheduleId());   // 토큰
        Long seatId = seatRepository.findSeatIdByScheduleIdAndSeatNumber(schedule.getScheduleId(), seatNumber);

        // 분산 락
        String lockKey = "seat-lock:" + seatId;   // 좌석 id로 락
        String lockValue = UUID.randomUUID().toString();

        int tryCount = 0;   // 시도 수

        while (true) {
            boolean locked = distributedLockRepository.tryLock(lockKey, lockValue, LOCK_TIMEOUT_MILLIS);
            if (locked) {   // 락 획득 성공
                try {
                    return reserveSeatTransactional(user.getUserId(), seatId, seatNumber);    // 좌석 임시 예약 처리
                } finally {
                    distributedLockRepository.unlock(lockKey, lockValue);   // 락 해제
                }
            } else {        // 락 획득 실패
                tryCount++;
                if (tryCount >= MAX_RETRY) {
                    throw new ApiException(ErrorCode.FORBIDDEN, "동일 좌석(" + seatNumber + ")에 대한 예약이 이미 진행 중입니다. 잠시 후 다시 시도해주세요.");
                }
                try {
                    Thread.sleep(SLEEP_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "락 대기 중 인터럽트 발생");
                }
            }
        }
    }


    /**
     * # Method설명 : 사용자 userId 검증
     * # MethodName : validateUser
     **/
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID(" + userId + ")의 사용자를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 콘서트 일정 검증
     * # MethodName : validateSchedule
     **/
    private ConcertSchedule validateSchedule(LocalDate concertDate) {
        ConcertSchedule schedule = scheduleRepository.findByConcertDate(concertDate)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 날짜(" + concertDate + ")에 해당하는 콘서트가 존재하지 않습니다."));
        if (schedule.getConcertDate().isBefore(LocalDate.now())) {  // 이미 지난 날짜
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "선택한 날짜(" + concertDate + ")에 해당하는 콘서트는 예약할 수 없습니다.");
        }
        return schedule;
    }

    /**
     * # Method설명 : 대기열 토큰 검증
     * # MethodName : validateQueueToken
     **/
    private QueueToken validateQueueToken(Long userId, Long scheduleId) {
        Optional<String> tokenIdOpt = queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, scheduleId);    // userId, scheduleId로 토큰 조회
        if (tokenIdOpt.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "사용자 ID(" + userId + ")로 발급된 대기열 토큰이 존재하지 않습니다.");
        }

        QueueToken queueToken = queueTokenRepository.findQueueTokenByTokenId(tokenIdOpt.get()).orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 대기열 토큰입니다. " + tokenIdOpt.get()));
        if (queueToken.isExpired() || !queueToken.isActive()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "대기열 토큰이 만료되었거나 예약 가능 상태가 아닙니다.");
        }
        return queueToken;
    }


    /**
     * # Method설명 : 예약 트랜잭션
     * # MethodName : reserveSeatTransactional
     **/
    @Transactional
    public SeatReservation reserveSeatTransactional(Long userId, Long seatId, int seatNumber) {

        // 락 획득 후 다시 좌석 조회 (락 획득 전에 조회한 좌석은 예약 완료된 상태가 적용되지 않음)
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 좌석(" + seatNumber + ")은 존재하지 않습니다."));

        // 만료된 임시예약 해제 (좌석 상태 갱신)
        if (seat.getStatus() == SeatStatus.TEMP_RESERVED && seat.isExpired(RESERVATION_TIMEOUT_MINUTES)) {
            seat.releaseAssignment();
            seatRepository.save(seat);
        }

        // 예약 불가능한 상태라면 예외
        if (!seat.isAvailable(RESERVATION_TIMEOUT_MINUTES)) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "선택한 좌석(" + seatNumber + ")은 이미 예약된 좌석입니다.");
        }

        // 임시 예약 처리
        seat.reserveTemporarily();
        seatRepository.save(seat);

        // 임시 예약 객체 생성/저장
        SeatReservation reservation = SeatReservation.createTemporary(userId, seat.getSeatId(), RESERVATION_TIMEOUT_MINUTES);
        return reservationRepository.save(reservation);
    }
}
