package kr.hhplus.be.server.application.batch;

import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatReservationBatch {

    private final SeatReservationRepository seatReservationRepository;
    private final SeatRepository seatRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final DistributedLockRepository distributedLockRepository;

    private static final long LOCK_TIMEOUT_MILLIS = 15000;
    private static final int MAX_RETRY = 10;
    private static final long SLEEP_MILLIS = 200;


    @Scheduled(fixedDelay = 100_000)
    public void expireTempReservations() {
        LocalDateTime now = LocalDateTime.now();
        // 임시예약 상태(TEMP_RESERVED)면서 만료시각이 현재보다 지난 예약 찾기
        List<SeatReservation> expiredReservations = seatReservationRepository.findByStatusAndExpiredAtBefore(ReservationStatus.TEMP_RESERVED, now);

        for (SeatReservation reservation : expiredReservations) {

            // 락 획득
            List<String> lockKeys = Arrays.asList(
                    "reservation-lock:" + reservation.getReservationId(),
                    "seat-lock:" + reservation.getSeatId()
            );

            distributedLockRepository.withMultiLock(lockKeys, () -> {
                expireReservationAndSeatTransactional(reservation.getReservationId(), reservation.getSeatId());
                return null;
            }, LOCK_TIMEOUT_MILLIS, MAX_RETRY, SLEEP_MILLIS);
        }
    }

    @Transactional
    public void expireReservationAndSeatTransactional(Long reservationId, Long seatId) {

        // 1. 예약 조회
        Optional<SeatReservation> reservationOpt = seatReservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            log.warn("예약 정보 없음: {}", reservationId);
            return;
        }
        SeatReservation reservation = reservationOpt.get();
        if (reservation.getStatus() != ReservationStatus.TEMP_RESERVED) {   // 임시 예약 확인
            return;
        }

        // 2. 좌석 조회
        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (seatOpt.isEmpty()) {
            log.warn("좌석 정보 없음: {}", seatId);
            return;
        }
        Seat seat = seatOpt.get();

        // 3. 상태 체크 및 만료 처리
        if (seat.getStatus() == SeatStatus.TEMP_RESERVED) {
            // 예약 만료 처리
            reservation.expireReservation();
            seatReservationRepository.save(reservation);

            // 좌석 만료 처리
            seat.releaseAssignment();
            seatRepository.save(seat);

            // 토큰 만료
            Long scheduleId = seat.getScheduleId();
            queueTokenRepository.findTokenIdByUserIdAndScheduleId(reservation.getUserId(), scheduleId)
                    .ifPresent(queueTokenRepository::expiresQueueToken);
        }
    }
}