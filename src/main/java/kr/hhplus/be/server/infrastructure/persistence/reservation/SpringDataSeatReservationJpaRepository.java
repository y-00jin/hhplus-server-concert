package kr.hhplus.be.server.infrastructure.persistence.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataSeatReservationJpaRepository extends JpaRepository<SeatReservationEntity, Long> {

    Optional<SeatReservationEntity> findByReservationIdAndUser_UserId(Long reservationId, Long userId);
}