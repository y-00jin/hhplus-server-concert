package kr.hhplus.be.server.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
}