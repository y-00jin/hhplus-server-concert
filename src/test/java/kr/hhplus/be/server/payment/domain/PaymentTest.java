package kr.hhplus.be.server.payment.domain;


import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    @Test
    void 결제_생성_성공() {
        // given
        Long userId = 1L;
        Long reservationId = 10L;
        long amount = 30000L;
        PaymentStatus status = PaymentStatus.SUCCESS;

        // when
        Payment payment = Payment.create(userId, reservationId, amount, status);

        // then
        assertThat(payment.getPaymentId()).isNull();
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getReservationId()).isEqualTo(reservationId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getStatus()).isEqualTo(status);
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
    }


    @Test
    void 생성자_정상_생성() {
        // given
        Long paymentId = 1L;
        Long userId = 2L;
        Long reservationId = 3L;
        long amount = 40000L;
        PaymentStatus status = PaymentStatus.SUCCESS;
        LocalDateTime now = LocalDateTime.now();

        // when
        Payment payment = new Payment(paymentId, userId, reservationId, amount, status, now, now);

        // then
        assertThat(payment.getPaymentId()).isEqualTo(paymentId);
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getReservationId()).isEqualTo(reservationId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getStatus()).isEqualTo(status);
        assertThat(payment.getCreatedAt()).isEqualTo(now);
        assertThat(payment.getUpdatedAt()).isEqualTo(now);
    }
}