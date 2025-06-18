package kr.hhplus.be.server.application.batch;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.ConcertSchedule;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatReservationBatch {

    private final SeatReservationRepository seatReservationRepository;
    private final SeatRepository seatRepository;
    private final QueueTokenRepository queueTokenRepository;

    @Scheduled(fixedDelay = 100_000)
    public void expireTempReservations() {
        LocalDateTime now = LocalDateTime.now();
        // 임시예약 상태(TEMP_RESERVED)면서 만료시각이 현재보다 지난 예약 찾기
        List<SeatReservation> expiredReservations =
                seatReservationRepository.findByStatusAndExpiredAtBefore(ReservationStatus.TEMP_RESERVED, now);

        for (SeatReservation reservation : expiredReservations) {
            try {

            if (reservation.getStatus() != ReservationStatus.TEMP_RESERVED) {
                continue;
            }

            // 예약 상태를 EXPIRED로 변경
            reservation.expireReservation();
            seatReservationRepository.save(reservation);

            // 좌석 상태 FREE로 변경
            Seat seat = seatRepository.findById(reservation.getSeatId()).orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null));
            if (seat.getStatus() == SeatStatus.TEMP_RESERVED) {
                seat.releaseAssignment();
                seatRepository.save(seat);
            }

            // 토큰 제거
            Long scheduleId = seat.getScheduleId();
            queueTokenRepository.findTokenIdByUserIdAndScheduleId(reservation.getUserId(), scheduleId)
                    .ifPresent(queueTokenRepository::expiresQueueToken);

            } catch (OptimisticLockingFailureException e) {
                log.error("[ERROR] 동시성 충돌로 예약 만료 스킵: reservationId={}", reservation.getReservationId());
            }
        }
    }
}