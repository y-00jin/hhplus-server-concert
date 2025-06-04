package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

    // 오늘 이후의 콘서트 일정만 조회
    List<ConcertSchedule> findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate today);

    // 특정 날짜로 콘서트 일정 조회
    Optional<ConcertSchedule> findByConcertDate(LocalDate concertDate);
}
