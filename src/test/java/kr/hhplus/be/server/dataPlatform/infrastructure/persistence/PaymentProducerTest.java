package kr.hhplus.be.server.dataPlatform.infrastructure.persistence;

import kr.hhplus.be.server.common.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentProducerTest {

    @Test
    void 결제성공_이벤트_발행_시_KafkaTemplate_send호출() {
        // given
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        PaymentProducer producer = new PaymentProducer(kafkaTemplate);

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                1L, 1L, 1L, 30000, null
        );

        // send 호출시 null 반환(CompletableFuture)
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mock(java.util.concurrent.CompletableFuture.class));

        // when
        producer.sendPaymentInfo(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, atLeastOnce()).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopicConstants.PAYMENT_SUCCESS);
        assertThat(valueCaptor.getValue()).isEqualTo(event);
    }
}