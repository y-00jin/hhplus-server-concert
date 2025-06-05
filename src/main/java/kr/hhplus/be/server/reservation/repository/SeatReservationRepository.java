package kr.hhplus.be.server.reservation.repository;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

}
