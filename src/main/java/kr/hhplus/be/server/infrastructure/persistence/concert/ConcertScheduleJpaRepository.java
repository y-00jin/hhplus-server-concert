package kr.hhplus.be.server.infrastructure.persistence.concert;

import kr.hhplus.be.server.domain.concert.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ConcertScheduleJpaRepository implements ConcertScheduleRepository {

    private final SpringDataConcertScheduleJpaRepository scheduleRepository;

    public ConcertScheduleJpaRepository(SpringDataConcertScheduleJpaRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public List<ConcertSchedule> findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate concertDate) {
        return scheduleRepository.findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(concertDate)
                .stream().map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ConcertSchedule> findByConcertDate(LocalDate concertDate) {
        return scheduleRepository.findByConcertDate(concertDate).map(this::toDomain);
    }


    private ConcertScheduleEntity toEntity(ConcertSchedule domain) {
        return ConcertScheduleEntity.builder()
                .scheduleId(domain.getScheduleId())
                .concertDate(domain.getConcertDate())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ConcertSchedule toDomain(ConcertScheduleEntity entity) {
        return new ConcertSchedule(
                entity.getScheduleId(),
                entity.getConcertDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
