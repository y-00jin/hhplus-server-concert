package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.repository.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.model.SeatReservation;
import kr.hhplus.be.server.reservation.domain.repository.SeatReservationRepository;
import kr.hhplus.be.server.reservation.infrastructure.persistence.SeatReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.SpringDataSeatReservationJpaRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentJpaRepository implements PaymentRepository {

    private final SpringDataPaymentJpaRepository jpaRepository;
    private final UserRepository userRepository;
    private final SpringDataSeatReservationJpaRepository seatReservationJpaRepository;


    public PaymentJpaRepository(SpringDataPaymentJpaRepository jpaRepository, UserRepository userRepository, SpringDataSeatReservationJpaRepository seatReservationJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.userRepository = userRepository;
        this.seatReservationJpaRepository = seatReservationJpaRepository;
    }
    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity saved = jpaRepository.save(entity);
        Payment result = toDomain(saved);
        result.assignId(saved.getPaymentId());
        return result;
    }

    private PaymentEntity toEntity(Payment p) {

        User user = userRepository.findById(p.getUserId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));
        SeatReservationEntity seatReservation = seatReservationJpaRepository.findById(p.getReservationId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));

        return PaymentEntity.builder()
                .paymentId(p.getPaymentId())
                .user(user)
                .seatReservation(seatReservation)
                .amount(p.getAmount())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private Payment toDomain(PaymentEntity e) {
        return new Payment(
                e.getPaymentId(),
                e.getUser().getUserId(),
                e.getSeatReservation().getReservationId(),
                e.getAmount(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
