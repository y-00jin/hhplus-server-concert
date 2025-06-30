package kr.hhplus.be.server.domain.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatReservationRepository {
    SeatReservation save(SeatReservation seatReservation);
    Optional<SeatReservation> findById(Long reservationId);
    Optional<SeatReservation> findByIdForUpdate(Long reservationId);    // lock
    Optional<SeatReservation> findByReservationIdAndUser_UserId(Long reservationId, Long userId);
    List<SeatReservation> findByStatusAndExpiredAtBefore(ReservationStatus status, LocalDateTime expiredAt);
    Long findSeatIdById(Long reservationId);
    void deleteAllForTest();
}
