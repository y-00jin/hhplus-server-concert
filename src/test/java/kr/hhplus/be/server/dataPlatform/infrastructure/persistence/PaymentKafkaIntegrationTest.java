package kr.hhplus.be.server.dataPlatform.infrastructure.persistence;


import kr.hhplus.be.server.common.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {KafkaTopicConstants.PAYMENT_SUCCESS})
@ActiveProfiles("test")
class PaymentKafkaIntegrationTest {

    @Autowired
    PaymentProducer paymentProducer;

    // 컨슈머에서 수신한 메시지를 모을 큐 (테스트 전용)
    private static final BlockingQueue<PaymentSuccessEvent> events = new LinkedBlockingQueue<>();

    @KafkaListener(
            topics = KafkaTopicConstants.PAYMENT_SUCCESS,
            groupId = "test-consumer-group"
    )
    void testKafkaListener(ConsumerRecord<String, PaymentSuccessEvent> record) {
        events.add(record.value());
    }

    @AfterEach
    void cleanQueue() {
        events.clear();
    }

    @Test
    void 프로듀서_메시지_발행시_컨슈머에서_정상수신한다() throws Exception {
        // given
        PaymentSuccessEvent event = new PaymentSuccessEvent(123L, 456L, 789L, 30000, null);

        // when
        paymentProducer.sendPaymentInfo(event);

        // then: 컨슈머가 메시지 수신했는지 확인 (최대 3초 대기)
        PaymentSuccessEvent received = events.poll(3, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.getPaymentId()).isEqualTo(123L);
        assertThat(received.getUserId()).isEqualTo(456L);
        assertThat(received.getReservationId()).isEqualTo(789L);
        assertThat(received.getAmount()).isEqualTo(30000);
    }
}