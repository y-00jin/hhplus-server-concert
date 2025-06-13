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

@Transactional(readOnly = true)
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

    // 좌석 예약 유스케이스 (사용자ID, 콘서트 날짜, 좌석 번호)
    @Transactional
    public SeatReservation reserveSeat(Long userId, LocalDate concertDate, int seatNumber) {

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다."));

        // 콘서트 일정 조회
        ConcertSchedule schedule = scheduleRepository.findByConcertDate(concertDate)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 날짜에 해당하는 콘서트가 존재하지 않습니다."));

        // 콘서트 날짜가 이미 지난 경우 예외 발생
        if (schedule.getConcertDate().isBefore(LocalDate.now())) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "선택한 날짜에 해당하는 콘서트는 예약할 수 없습니다.");
        }

        // 대기열 토큰 검증 (userId, scheduleId 기준)
        Optional<String> tokenIdOpt = queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, schedule.getScheduleId());
        if (tokenIdOpt.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "대기열을 통과하지 않은 사용자입니다.");
        }
        QueueToken queueToken = queueTokenRepository.findQueueTokenByTokenId(tokenIdOpt.get())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 대기열 토큰입니다."));
        if (queueToken.isExpired() || !queueToken.isActive()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "대기열 토큰이 만료되었거나 입장 상태가 아닙니다.");
        }

        // 좌석 락 획득 (좌석 단위 분산락)
        String lockKey = "seat-lock:" + schedule.getScheduleId() + ":" + seatNumber;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = distributedLockRepository.tryLock(lockKey, lockValue, 10_000);
        if (!locked) {
            throw new ApiException(ErrorCode.FORBIDDEN, "동일 좌석에 대한 예약이 이미 진행 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 좌석 조회
            Seat seat = seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumber(schedule.getScheduleId(), seatNumber)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 좌석은 존재하지 않거나 이미 예약된 좌석입니다."));

            // 임시예약 만료좌석 해제 -> 예약 가능 상태로 바꿔줌
            if (!seat.isAvailable(RESERVATION_TIMEOUT_MINUTES)) {   // 예약 불가능한 상태
                if (seat.isExpired(RESERVATION_TIMEOUT_MINUTES)) {  // TEMP_RESERVED(임시 예약) 상태지만 만료된 경우
                    seat.releaseAssignment();   // 임시예약 해제
                    seatRepository.save(seat);  // 저장
                } else {
                    throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "이미 예약된 좌석입니다.");
                }
            }

            // 좌석 임시예약
            seat.reserveTemporarily();
            seatRepository.save(seat);

            // 임시 예약 객체 생성/저장
            SeatReservation reservation = SeatReservation.createTemporary(user.getUserId(), seat.getSeatId(), RESERVATION_TIMEOUT_MINUTES);
            SeatReservation savedReservation = reservationRepository.save(reservation);


            // 예약 성공 시 대기열 토큰 만료
//            queueTokenRepository.expiresQueueToken(tokenIdOpt.get());

            return savedReservation;

        } finally {
            // 9. 락 해제
            distributedLockRepository.unlock(lockKey, lockValue);
        }

    }

}
