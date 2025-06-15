package kr.hhplus.be.server.infrastructure.persistence.concert;

import kr.hhplus.be.server.domain.concert.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataSeatJpaRepository extends JpaRepository<SeatEntity, Long> {

    // 예약 가능 좌석만 조회
    List<SeatEntity> findAllByConcertSchedule_ScheduleIdAndStatus(Long scheduleId, SeatStatus status);

    // 일정ID, 좌석번호, 상태로 조회
    Optional<SeatEntity> findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(Long scheduleId, int seatNumber, SeatStatus status);

    Optional<SeatEntity> findByConcertSchedule_ScheduleIdAndSeatNumber(Long scheduleId, int seatNumber);

}