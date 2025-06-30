package kr.hhplus.be.server.infrastructure.persistence.concert;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataSeatJpaRepository extends JpaRepository<SeatEntity, Long> {

    // 예약 가능 좌석만 조회
    List<SeatEntity> findAllByConcertSchedule_ScheduleIdAndStatus(Long scheduleId, SeatStatus status);

    // 일정ID, 좌석번호, 상태로 조회
    Optional<SeatEntity> findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(Long scheduleId, int seatNumber, SeatStatus status);

    // 일정ID, 좌석번호로 좌석ID만 조회
    @Query("select s.seatId from SeatEntity s where s.concertSchedule.scheduleId = :scheduleId and s.seatNumber = :seatNumber")
    Optional<Long> findSeatIdByScheduleIdAndSeatNumber(@Param("scheduleId") Long scheduleId, @Param("seatNumber") int seatNumber);

    // 일정ID, 좌석번호로 조회
    Optional<SeatEntity> findByConcertSchedule_ScheduleIdAndSeatNumber(Long scheduleId, int seatNumber);

    // 일정ID, 좌석번호로 조회 (비관락 적용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatEntity s WHERE s.concertSchedule.scheduleId = :scheduleId AND s.seatNumber = :seatNumber")
    Optional<SeatEntity> findByConcertSchedule_ScheduleIdAndSeatNumberForUpdate(@Param("scheduleId") Long scheduleId, @Param("seatNumber") int seatNumber);

    // 좌석ID로 조회 (비관락 적용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatEntity s WHERE s.seatId = :seatId")
    Optional<SeatEntity> findBySeatIdForUpdate(@Param("seatId") Long seatId);

}