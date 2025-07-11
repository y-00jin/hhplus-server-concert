package kr.hhplus.be.server.application.payment.event;

import kr.hhplus.be.server.domain.payment.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(PaymentSuccessEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

}
