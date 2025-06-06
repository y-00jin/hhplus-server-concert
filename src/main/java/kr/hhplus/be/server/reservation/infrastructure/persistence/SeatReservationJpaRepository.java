package kr.hhplus.be.server.reservation.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.reservation.domain.model.SeatReservation;
import kr.hhplus.be.server.reservation.domain.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SeatReservationJpaRepository implements SeatReservationRepository {

    private final SpringDataSeatReservationJpaRepository jpaRepository;

    private final UserRepository userRepository;
    private final SeatRepository seatRepository;


    public SeatReservationJpaRepository(SpringDataSeatReservationJpaRepository jpaRepository, UserRepository userRepository, SeatRepository seatRepository) {
        this.jpaRepository = jpaRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    public SeatReservation save(SeatReservation seatReservation) {
        SeatReservationEntity entity = toEntity(seatReservation);
        SeatReservationEntity saved = jpaRepository.save(entity);
        SeatReservation result = toDomain(saved);
        result.assignId(saved.getReservationId());
        return result;
    }

    @Override
    public Optional<SeatReservation> findById(Long reservationId) {
        return jpaRepository.findById(reservationId).map(this::toDomain);
    }


    private SeatReservationEntity toEntity(SeatReservation sr) {

        // 1. id로 User, Seat 객체 조회
        User user = userRepository.findById(sr.getUserId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));
        Seat seat = seatRepository.findById(sr.getSeatId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));

        return SeatReservationEntity.builder()
                .reservationId(sr.getReservationId())
                .user(user)
                .seat(seat)
                .status(sr.getStatus())
                .expiredAt(sr.getExpiredAt())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }

    private SeatReservation toDomain(SeatReservationEntity e) {
        return new SeatReservation(
                e.getReservationId(),
                e.getUser().getUserId(),
                e.getSeat().getSeatId(),
                e.getStatus(),
                e.getExpiredAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
