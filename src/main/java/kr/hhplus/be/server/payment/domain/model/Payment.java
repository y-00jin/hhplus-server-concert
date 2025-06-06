package kr.hhplus.be.server.payment.domain.model;

import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import java.time.LocalDateTime;

public class Payment {
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private long amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment(Long paymentId, Long userId, Long reservationId, long amount, PaymentStatus status,  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(Long userId, Long reservationId, long amount, PaymentStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Payment(null, userId, reservationId, amount, status, now, now);
    }

    public void assignId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}