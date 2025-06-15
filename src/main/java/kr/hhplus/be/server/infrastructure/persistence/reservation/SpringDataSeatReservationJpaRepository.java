package kr.hhplus.be.server.infrastructure.persistence.reservation;

import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataSeatReservationJpaRepository extends JpaRepository<SeatReservationEntity, Long> {

    Optional<SeatReservationEntity> findByReservationIdAndUser_UserId(Long reservationId, Long userId);

    List<SeatReservationEntity> findByStatusAndExpiredAtBefore(ReservationStatus status, LocalDateTime expiredAt);
}