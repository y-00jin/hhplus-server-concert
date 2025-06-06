package kr.hhplus.be.server.reservation.domain.repository;

import kr.hhplus.be.server.reservation.domain.model.SeatReservation;

import java.util.Optional;

public interface SeatReservationRepository {
    SeatReservation save(SeatReservation seatReservation);
    Optional<SeatReservation> findById(Long reservationId);
}
