package kr.hhplus.be.server.dataPlatform.infrastructure.persistence;

import kr.hhplus.be.server.common.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataPlatformClient {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_RETRY = 5; // 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000; // 재시도 시 대기시간

    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor();   // 단일 스레드 스케줄러(재시도용)

    public void sendPaymentInfo(PaymentSuccessEvent event) {
        sendPaymentInfoWithRetry(event, MAX_RETRY);
    }

    private void sendPaymentInfoWithRetry(PaymentSuccessEvent event, int retries) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KafkaTopicConstants.PAYMENT_SUCCESS, event.getPaymentId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) { // 전송 성공
                log.info("데이터플랫폼 전송 성공: paymentId={}, offset={}", event.getPaymentId(), result.getRecordMetadata().offset());
            } else { // 전송 실패
                if (retries > 0) {
                    log.warn("데이터플랫폼 전송 실패: paymentId={}, 원인={}, 재시도 남은 횟수={}", event.getPaymentId(), ex.getMessage(), retries - 1);
                    // 대기 후 재시도
                    retryScheduler.schedule(
                            () -> sendPaymentInfoWithRetry(event, retries - 1),
                            RETRY_DELAY_MS,
                            TimeUnit.MILLISECONDS
                    );
                } else {
                    log.error("데이터플랫폼 최종 전송 실패: paymentId={}, 원인={}", event.getPaymentId(), ex.getMessage(), ex);
                }
            }
        });
    }

}
