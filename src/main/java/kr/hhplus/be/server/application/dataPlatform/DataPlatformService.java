package kr.hhplus.be.server.application.dataPlatform;

import kr.hhplus.be.server.domain.payment.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataPlatformService {

    public void sendPaymentInfo(PaymentSuccessEvent event) {
        log.info("데이터플랫폼 Mock API 호출: {}", event.getPaymentId());
    }
}
