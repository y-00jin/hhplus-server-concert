package kr.hhplus.be.server.infrastructure.persistence.reservation;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.infrastructure.persistence.concert.SeatEntity;
import kr.hhplus.be.server.infrastructure.persistence.concert.SpringDataSeatJpaRepository;
import kr.hhplus.be.server.infrastructure.persistence.user.SpringDataUserJpaRepository;
import kr.hhplus.be.server.infrastructure.persistence.user.UserEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SeatReservationJpaRepository implements SeatReservationRepository {

    private final SpringDataSeatReservationJpaRepository seatReservationRepository;
    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataSeatJpaRepository seatRepository;


    public SeatReservationJpaRepository(SpringDataSeatReservationJpaRepository seatReservationRepository, SpringDataUserJpaRepository userRepository, SpringDataSeatJpaRepository seatRepository) {
        this.seatReservationRepository = seatReservationRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    public SeatReservation save(SeatReservation seatReservation) {
        SeatReservationEntity saved = seatReservationRepository.save(toEntity(seatReservation));
        return toDomain(saved);
    }

    @Override
    public Optional<SeatReservation> findById(Long reservationId) {
        return seatReservationRepository.findById(reservationId).map(this::toDomain);
    }

    @Override
    public Optional<SeatReservation> findByReservationIdAndUser_UserId(Long reservationId, Long userId) {
        return seatReservationRepository.findByReservationIdAndUser_UserId(reservationId, userId).map(this::toDomain);
    }

    @Override
    public List<SeatReservation> findByStatusAndExpiredAtBefore(ReservationStatus status, LocalDateTime expiredAt) {
        return seatReservationRepository.findByStatusAndExpiredAtBefore(status, expiredAt);
    }


    private SeatReservationEntity toEntity(SeatReservation domain) {

        // 1. id로 User, Seat 객체 조회
        UserEntity user = userRepository.findById(domain.getUserId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));
        SeatEntity seat = seatRepository.findById(domain.getSeatId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));

        return SeatReservationEntity.builder()
                .reservationId(domain.getReservationId())
                .user(user)
                .seat(seat)
                .status(domain.getStatus())
                .expiredAt(domain.getExpiredAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private SeatReservation toDomain(SeatReservationEntity entity) {
        return new SeatReservation(
                entity.getReservationId(),
                entity.getUser().getUserId(),
                entity.getSeat().getSeatId(),
                entity.getStatus(),
                entity.getExpiredAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
