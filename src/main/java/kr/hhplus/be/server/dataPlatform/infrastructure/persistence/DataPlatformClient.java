package kr.hhplus.be.server.dataPlatform.infrastructure.persistence;

import kr.hhplus.be.server.common.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataPlatformClient {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentInfo(PaymentSuccessEvent event) {
        kafkaTemplate.send(KafkaTopicConstants.PAYMENT_SUCCESS, event.getPaymentId().toString(), event);
        log.info("데이터플랫폼 Mock API 호출: {}", event.getPaymentId());
    }

}
