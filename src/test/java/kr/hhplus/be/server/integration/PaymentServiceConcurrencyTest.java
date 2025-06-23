package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.queue.QueueStatus;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class PaymentServiceConcurrencyTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    ConcertScheduleRepository scheduleRepository;
    @Autowired
    SeatRepository seatRepository;
    @Autowired
    QueueTokenRepository queueTokenRepository;
    @Autowired
    SeatReservationRepository seatReservationRepository;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    UserBalanceRepository userBalanceRepository;

    @Autowired
    PaymentService paymentService;


    private Long userId;
    private Long reservationId;
    private LocalDateTime now;

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAllForTest();
        seatReservationRepository.deleteAllForTest();
        userBalanceRepository.deleteAllForTest();
        seatRepository.deleteAllForTest();
        scheduleRepository.deleteAllForTest();
        userRepository.deleteAllForTest();
    }

    @BeforeEach
    void setup() {
        now = LocalDateTime.now();

        // 사용자 등록
        userId = userRepository.save(new User(null, UUID.randomUUID().toString(), "test@email.com", "pw", "사용자1", now, now)).getUserId();

        // 포인트 충전
        userBalanceRepository.save(UserBalance.charge(userId, 100000L, 0L));

        // 콘서트 일정 및 좌석 등록 (좌석은 임시 예약 상태로)
        Long scheduleId = scheduleRepository.save(new ConcertSchedule(null, LocalDate.of(2025, 7, 1), now, now)).getScheduleId();
        Long seatId = seatRepository.save(new Seat(null, scheduleId, 1, 30000, SeatStatus.FREE , now, now)).getSeatId();

        // 대기열 토큰 발급
        queueTokenRepository.save(new QueueToken(UUID.randomUUID().toString(), userId, scheduleId, 0, QueueStatus.ACTIVE, now, now.minusMinutes(5)));

        // 임시 예약 생성
        reservationId = seatReservationRepository.save(new SeatReservation(null, userId, seatId, ReservationStatus.TEMP_RESERVED, now.plusMinutes(5), now, now)).getReservationId();

    }

    @Test
    void 결제_동시성_테스트() throws Exception {

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Payment result = paymentService.payment(userId, reservationId);
                    results.add(true);
                    log.info("결제 성공 - {}", result.getPaymentId());
                } catch (Exception e) {
                    results.add(false);
                    log.error("결제 실패 ");
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        // 한 번만 성공, 나머지는 실패
        long successCount = results.stream().filter(r -> r).count();
        long failCount = results.stream().filter(r -> !r).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(threadCount - successCount);
    }
}
