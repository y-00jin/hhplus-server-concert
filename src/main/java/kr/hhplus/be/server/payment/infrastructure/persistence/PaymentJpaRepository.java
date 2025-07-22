package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentRepository;
import kr.hhplus.be.server.reservation.infrastructure.persistence.SeatReservationEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.SpringDataSeatReservationJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.user.SpringDataUserJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.user.UserEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentJpaRepository implements PaymentRepository {

    private final SpringDataPaymentJpaRepository paymentRepository;
    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataSeatReservationJpaRepository seatReservationJpaRepository;

    public PaymentJpaRepository(SpringDataPaymentJpaRepository paymentRepository, SpringDataUserJpaRepository userRepository, SpringDataSeatReservationJpaRepository seatReservationJpaRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.seatReservationJpaRepository = seatReservationJpaRepository;
    }
    @Override
    public Payment save(Payment payment) {
        PaymentEntity saved = paymentRepository.save(toEntity(payment));
        return toDomain(saved);
    }

    @Override
    public void deleteAllForTest() {
        paymentRepository.deleteAll();
    }

    private PaymentEntity toEntity(Payment p) {

        UserEntity user = userRepository.findById(p.getUserId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));
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
