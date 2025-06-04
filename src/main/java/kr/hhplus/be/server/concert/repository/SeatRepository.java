package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 예약 가능 좌석만 조회
    List<Seat> findAllByConcertSchedule_ScheduleIdAndStatus(Long scheduleId, SeatStatus status);

    // 전체 좌석 (status 상관없이)
    List<Seat> findAllByConcertSchedule_ScheduleId(Long scheduleId);

}
