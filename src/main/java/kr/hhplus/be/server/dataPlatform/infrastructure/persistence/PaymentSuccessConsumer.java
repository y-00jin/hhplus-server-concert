package kr.hhplus.be.server.dataPlatform.infrastructure.persistence;

import kr.hhplus.be.server.common.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PaymentSuccessConsumer {
    private static final Set<String> processedPaymentIds = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = KafkaTopicConstants.PAYMENT_SUCCESS,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, PaymentSuccessEvent> record) {
        PaymentSuccessEvent event = record.value();
        String paymentId = event.getPaymentId().toString();

        // 멱등성: 이미 처리한 메시지면 무시
        if (!processedPaymentIds.add(paymentId)) {
            log.warn("Kafka 컨슈머: 중복 메시지! paymentId={} 이미 처리됨, skip", paymentId);
            return;
        }

        // 실제 처리 로직
        log.info("Kafka 컨슈머: 결제 성공 이벤트 정상 처리: paymentId={}", paymentId);
    }
}