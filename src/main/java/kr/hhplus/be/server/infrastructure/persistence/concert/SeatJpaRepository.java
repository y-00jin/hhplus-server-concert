package kr.hhplus.be.server.infrastructure.persistence.concert;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SeatJpaRepository implements SeatRepository {

    private final SpringDataSeatJpaRepository seatRepository;
    private final SpringDataConcertScheduleJpaRepository scheduleRepository;

    public SeatJpaRepository(SpringDataSeatJpaRepository seatRepository, SpringDataConcertScheduleJpaRepository scheduleRepository) {
        this.seatRepository = seatRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public List<Seat> findAllByConcertSchedule_ScheduleIdAndStatus(Long scheduleId, SeatStatus status) {
        return seatRepository.findAllByConcertSchedule_ScheduleIdAndStatus(scheduleId, status)
                .stream().map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(Long scheduleId, int seatNumber, SeatStatus status) {
        return seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(scheduleId, seatNumber, status).map(this::toDomain);
    }

    @Override
    public Optional<Seat> findByConcertSchedule_ScheduleIdAndSeatNumber(Long scheduleId, int seatNumber) {
        return seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumber(scheduleId, seatNumber).map(this::toDomain);
    }

    @Override
    public Seat save(Seat seat) {
        SeatEntity saved = seatRepository.save(toEntity(seat));
        return toDomain(saved);
    }

    @Override
    public Optional<Seat> findById(Long seatId) {
        return seatRepository.findById(seatId).map(this::toDomain);
    }

    @Override
    public void deleteAllForTest() {
        seatRepository.deleteAll();
    }


    private SeatEntity toEntity(Seat domain) {
        ConcertScheduleEntity scheduleEntity =  scheduleRepository.findById(domain.getScheduleId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));
        return SeatEntity.builder()
                .seatId(domain.getSeatId())
                .concertSchedule(scheduleEntity)
                .seatNumber(domain.getSeatNumber())
                .price(domain.getPrice())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    private Seat toDomain(SeatEntity entity) {
        return new Seat(
                entity.getSeatId(),
                entity.getConcertSchedule().getScheduleId(),
                entity.getSeatNumber(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
