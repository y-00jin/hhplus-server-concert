package kr.hhplus.be.server.infrastructure.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
}