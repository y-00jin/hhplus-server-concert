package kr.hhplus.be.server.payment.application.event;


import kr.hhplus.be.server.dataPlatform.infrastructure.persistence.PaymentProducer;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PaymentEventListenerTest {

    @Test
    void 결제성공이벤트_수신시_프로듀서_호출() {
        // given
        PaymentProducer mockProducer = mock(PaymentProducer.class);
        PaymentEventListener listener = new PaymentEventListener(mockProducer);

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                1L, 1L, 1L, 30000, null
        );

        // when
        listener.handlePaymentSuccessEvent(event);

        // then
        verify(mockProducer, times(1)).sendPaymentInfo(event);
    }
}