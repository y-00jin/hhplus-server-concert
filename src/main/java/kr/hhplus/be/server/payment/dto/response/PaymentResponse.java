package kr.hhplus.be.server.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private long amount;
    private String status;
    private LocalDateTime createdAt;
}