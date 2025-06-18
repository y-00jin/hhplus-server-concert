package kr.hhplus.be.server.application.batch;

import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SeatReservationBatch {

    private final SeatReservationRepository seatReservationRepository;

    // 5분(300초)마다 임시예약 만료 자동 처리
    @Scheduled(fixedDelay = 300_000)
    public void expireTempReservations() {
        LocalDateTime now = LocalDateTime.now();
        // 임시예약 상태(TEMP_RESERVED)면서 만료시각이 현재보다 지난 예약 찾기
        List<SeatReservation> expiredReservations =
                seatReservationRepository.findByStatusAndExpiredAtBefore(ReservationStatus.TEMP_RESERVED, now);

        for (SeatReservation reservation : expiredReservations) {
            reservation.expireReservation(); // 상태를 EXPIRED로 변경
            seatReservationRepository.save(reservation);
        }
    }

}
