package kr.hhplus.be.server.payment.application.event;

import kr.hhplus.be.server.dataPlatform.infrastructure.persistence.DataPlatformClient;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final DataPlatformClient dataPlatformClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        dataPlatformClient.sendPaymentInfo(event);
    }
}
