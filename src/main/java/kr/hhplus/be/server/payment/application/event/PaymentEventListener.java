package kr.hhplus.be.server.payment.application.event;

import kr.hhplus.be.server.application.dataPlatform.DataPlatformService;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final DataPlatformService dataPlatformService; // 데이터플랫폼 연동용 서비스

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        dataPlatformService.sendPaymentInfo(event);
    }
}
