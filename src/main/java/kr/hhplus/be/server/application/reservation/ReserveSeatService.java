package kr.hhplus.be.server.application.reservation;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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

    // 좌석 예약 메인 진입점
    public SeatReservation reserveSeat(Long userId, LocalDate concertDate, int seatNumber) {

        // 1. 검증 단계
        User user = validateUser(userId);
        ConcertSchedule schedule = validateSchedule(concertDate);
        QueueToken queueToken = validateQueueToken(userId, schedule.getScheduleId());


        // 2. 분산락 - 데드락 등의 문제를 줄이기 위해 트랜잭션 바깥에서 처리
        String lockKey = "seat-lock:" + schedule.getScheduleId() + ":" + seatNumber;
        String lockValue = UUID.randomUUID().toString();
        if (!distributedLockRepository.tryLock(lockKey, lockValue, 10_000)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "동일 좌석("+seatNumber+")에 대한 예약이 이미 진행 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 3. 좌석 임시예약 처리 (트랜잭션)
            return reserveSeatTransactional(user.getUserId(), schedule.getScheduleId(), seatNumber);
        } finally {
            // 4. 락 해제
            distributedLockRepository.unlock(lockKey, lockValue);
        }
    }


    /**
     * # Method설명 : 사용자 userId 검증
     * # MethodName : validateUser
     **/
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 콘서트 일정 검증
     * # MethodName : validateSchedule
     **/
    private ConcertSchedule validateSchedule(LocalDate concertDate) {
        ConcertSchedule schedule = scheduleRepository.findByConcertDate(concertDate)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 날짜("+ concertDate +")에 해당하는 콘서트가 존재하지 않습니다."));
        if (schedule.getConcertDate().isBefore(LocalDate.now())) {  // 이미 지난 날짜
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "선택한 날짜("+concertDate+")에 해당하는 콘서트는 예약할 수 없습니다.");
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
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "사용자 ID("+userId+")로 발급된 대기열 토큰이 존재하지 않습니다.");
        }
        QueueToken queueToken = queueTokenRepository.findQueueTokenByTokenId(tokenIdOpt.get())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 대기열 토큰입니다. " + tokenIdOpt.get()));
        if (queueToken.isExpired() || !queueToken.isActive()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "대기열 토큰이 만료되었거나 예약 가능 상태가 아닙니다.");
        }
        return queueToken;
    }


    @Transactional
    public SeatReservation reserveSeatTransactional(Long userId, Long scheduleId, int seatNumber) {

        // 좌석 조회
        Seat seat = seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumber(scheduleId, seatNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 좌석("+ seatNumber +")은 존재하지 않습니다."));

        // 만료된 임시예약 해제 (좌석 상태 갱신)
        if (seat.getStatus() == SeatStatus.TEMP_RESERVED && seat.isExpired(RESERVATION_TIMEOUT_MINUTES)) {
            seat.releaseAssignment();
            seatRepository.save(seat);
        }

        // 예약 불가능한 상태라면 예외
        if (!seat.isAvailable(RESERVATION_TIMEOUT_MINUTES)) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "선택한 좌석("+ seatNumber +")은 이미 예약된 좌석입니다.");
        }

        // 임시 예약 처리
        seat.reserveTemporarily();
        seatRepository.save(seat);

        // 임시 예약 객체 생성/저장
        SeatReservation reservation = SeatReservation.createTemporary(userId, seat.getSeatId(), RESERVATION_TIMEOUT_MINUTES);
        return reservationRepository.save(reservation);
    }
}
