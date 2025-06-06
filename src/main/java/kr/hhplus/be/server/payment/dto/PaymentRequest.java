package kr.hhplus.be.server.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRequest {
    private Long userId;
    private Long reservationId;
}
