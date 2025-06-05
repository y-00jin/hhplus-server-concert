package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 예약 가능 좌석만 조회
    List<Seat> findAllByConcertSchedule_ScheduleIdAndStatus(Long scheduleId, SeatStatus status);

    Optional<Seat> findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(Long scheduleId, int seatNumber, SeatStatus status);

}
