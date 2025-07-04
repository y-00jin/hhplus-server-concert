package kr.hhplus.be.server.domain.concert;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertScheduleRepository {

    // 오늘 이후의 콘서트 일정만 조회
    List<ConcertSchedule> findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate today);

    List<ConcertSchedule> findAllByScheduleIdIn(List<Long> scheduleIds);

    // 특정 날짜로 콘서트 일정 조회
    Optional<ConcertSchedule> findByConcertDate(LocalDate concertDate);

    // 일정ID로 콘서트 일정 조회
    Optional<ConcertSchedule> findById(Long scheduleId);

    boolean existsById(Long scheduleId);

    ConcertSchedule save(ConcertSchedule concertSchedule);

    void deleteAllForTest();

}
