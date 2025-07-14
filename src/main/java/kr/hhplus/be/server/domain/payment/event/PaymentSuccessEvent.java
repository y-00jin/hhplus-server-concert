package kr.hhplus.be.server.domain.payment.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class PaymentSuccessEvent {

    private final Long paymentId;
    private final Long userId;
    private final Long reservationId;
    private final long amount;
    private final LocalDateTime createdAt;
}
