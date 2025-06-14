package kr.hhplus.be.server.infrastructure.persistence.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataConcertScheduleJpaRepository extends JpaRepository<ConcertScheduleEntity, Long> {
    // 특정 날짜 이후의 콘서트 일정만 조회
    List<ConcertScheduleEntity> findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate concertDate);

    // 특정 날짜로 콘서트 일정 조회
    Optional<ConcertScheduleEntity> findByConcertDate(LocalDate concertDate);
}