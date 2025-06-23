package kr.hhplus.be.server.application.batch;

import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
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
    @Transactional
    public void expireTempReservations() {
        LocalDateTime now = LocalDateTime.now();
        // 임시예약 상태(TEMP_RESERVED)면서 만료시각이 현재보다 지난 예약 찾기
        List<SeatReservation> expiredReservations =
                seatReservationRepository.findByStatusAndExpiredAtBefore(ReservationStatus.TEMP_RESERVED, now);

        for (SeatReservation reservation : expiredReservations) {
            expireReservationAndSeat(reservation);
        }
    }

    @Transactional
    public void expireReservationAndSeat(SeatReservation reservation) {

        // 1. 비관적 락으로 예약 엔티티 다시 조회
        Optional<SeatReservation> lockedReservationOpt = seatReservationRepository.findByIdForUpdate(reservation.getReservationId());
        if(lockedReservationOpt.isEmpty()){
            log.warn("예약 정보 없음: {}", reservation.getReservationId());
            return;
        }

        SeatReservation lockedReservation = lockedReservationOpt.get();
        if (lockedReservation.getStatus() != ReservationStatus.TEMP_RESERVED) {
            return;
        }

        // 2. 좌석에 비관적 락 걸어서 select (seatId 사용)
        Optional<Seat> seatOpt = seatRepository.findBySeatIdForUpdate(reservation.getSeatId());
        if (seatOpt.isEmpty()) {
            log.warn("좌석 없음: {}", reservation.getSeatId());
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