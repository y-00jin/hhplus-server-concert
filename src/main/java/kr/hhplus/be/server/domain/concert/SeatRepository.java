package kr.hhplus.be.server.domain.concert;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {

    // 예약 가능 좌석만 조회
    List<Seat> findAllByConcertSchedule_ScheduleIdAndStatus(Long scheduleId, SeatStatus status);

    // 일정ID, 좌석번호, 상태로 조회
    Optional<Seat> findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(Long scheduleId, int seatNumber, SeatStatus status);

    // 일정ID, 좌석 번호로 좌석 ID만 조회
    Long findSeatIdByScheduleIdAndSeatNumber(Long scheduleId, int seatNumber);

    // 일정ID, 좌석 번호로 Seat 조회
    Optional<Seat> findByConcertSchedule_ScheduleIdAndSeatNumber(Long scheduleId, int seatNumber);

    Optional<Seat> findByConcertSchedule_ScheduleIdAndSeatNumberForUpdate(Long scheduleId, int seatNumber);

    Optional<Seat> findBySeatIdForUpdate(Long seatId);

    Seat save(Seat payment);

    Optional<Seat> findById(Long seatId);

    void deleteAllForTest();
}
