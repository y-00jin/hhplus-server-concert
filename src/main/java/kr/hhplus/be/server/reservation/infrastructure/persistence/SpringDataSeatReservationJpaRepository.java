package kr.hhplus.be.server.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSeatReservationJpaRepository extends JpaRepository<SeatReservationEntity, Long> {
}