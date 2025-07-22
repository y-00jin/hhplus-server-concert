package kr.hhplus.be.server.payment.domain.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentSuccessEvent {

    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private long amount;
    private LocalDateTime createdAt;

}
