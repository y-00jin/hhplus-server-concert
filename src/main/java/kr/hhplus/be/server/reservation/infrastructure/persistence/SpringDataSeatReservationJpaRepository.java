package kr.hhplus.be.server.reservation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataSeatReservationJpaRepository extends JpaRepository<SeatReservationEntity, Long> {

    // 예약ID로 비관락 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM SeatReservationEntity sr WHERE sr.reservationId = :reservationId")
    Optional<SeatReservationEntity> findByIdForUpdate(@Param("reservationId") Long reservationId);
    Optional<SeatReservationEntity> findByReservationIdAndUser_UserId(Long reservationId, Long userId);

    List<SeatReservationEntity> findByStatusAndExpiredAtBefore(ReservationStatus status, LocalDateTime expiredAt);

    @Query("SELECT sr.seat.seatId FROM SeatReservationEntity sr WHERE sr.reservationId = :reservationId")
    Optional<Long> findSeatIdById(@Param("reservationId") Long reservationId);
}