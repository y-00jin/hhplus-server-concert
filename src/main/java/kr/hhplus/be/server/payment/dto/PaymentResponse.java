package kr.hhplus.be.server.payment.dto;

import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private long amount;
    private PaymentStatus status;
}