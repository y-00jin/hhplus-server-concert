package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.batch.SeatReservationBatch;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class SeatReservationBatchConcurrencyTest {

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
    @Autowired
    SeatReservationBatch seatReservationBatch;

    private Long userId;
    private Long scheduleId;
    private Long seatId;
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
        LocalDate concertDate = LocalDate.now().plusDays(1);
        scheduleId = scheduleRepository.save(new ConcertSchedule(null, concertDate, now, now)).getScheduleId();
        seatId = seatRepository.save(new Seat(null, scheduleId, 1, 30000, SeatStatus.TEMP_RESERVED , now, now)).getSeatId();

        // 대기열 토큰 발급 (만료된 토큰)
        queueTokenRepository.save(new QueueToken(UUID.randomUUID().toString(), userId, scheduleId, 0, QueueStatus.ACTIVE, now.minusMinutes(6), now.minusMinutes(1)));
    }


    @Test
    void 예약_만료_동시성_테스트() throws Exception{

        // 예약 생성 (TEMP_RESERVED & 만료시각 지난 예약 1개 생성)
        LocalDateTime expiredAt = now.minusMinutes(1);  // 만료 시간 (현재 시간 1분전)
        LocalDateTime createdAt = now.minusMinutes(6);  // 생성 시간 (5분전)

        SeatReservation reservation = seatReservationRepository.save(
                new SeatReservation(null, userId, seatId, ReservationStatus.TEMP_RESERVED, expiredAt, createdAt, createdAt)
        );
        Long reservationId = reservation.getReservationId();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 1번 스레드: 예약 만료 상태로 변경
        executor.execute(() -> {
            try {
                seatReservationBatch.expireTempReservations();
                log.info("[ 좌석 만료 시도 ] reservationId : {}", reservationId);
            } catch (Exception e) {
                log.error("[ 좌석 만료 실패 ] {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        // 2번 스레드: 동시에 유저가 결제 확정 처리
        executor.execute(() -> {
            try {
                Payment result = paymentService.payment(userId, reservationId);
                log.info("[ 결제 성공 ] paymentId : {}", result.getPaymentId());
            } catch (Exception e) {
                log.error("[ 결제 실패 ] {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await();  // 두 스레드 모두 끝날 때까지 대기

        // then
        SeatReservation finalReservation = seatReservationRepository.findById(reservationId).orElseThrow();
        Seat finalSeat = seatRepository.findById(seatId).orElseThrow();

        // 둘 중 하나만 true여야 함 (둘 다 TEMP_RESERVED는 불가능)
        boolean isReservationExpired = finalReservation.getStatus() == ReservationStatus.EXPIRED;       // 예약 만료
        boolean isReservationConfirmed = finalReservation.getStatus() == ReservationStatus.CONFIRMED;   // 예약 확정
        boolean isSeatFree = finalSeat.getStatus() == SeatStatus.FREE;              // 좌석 예약 가능
        boolean isSeatConfirmed = finalSeat.getStatus() == SeatStatus.CONFIRMED;    // 좌석 예약 확정

        // 1. 예약/좌석 상태 조합이 둘 중 하나만 성공했는지 검증
        assertThat(isReservationExpired ^ isReservationConfirmed).isTrue();
        assertThat(isSeatFree ^ isSeatConfirmed).isTrue();

        // 2. 둘 다 TEMP_RESERVED가 아니고, 하나는 만료, 하나는 확정이어야 함
        assertThat((isReservationExpired && isSeatFree) || (isReservationConfirmed && isSeatConfirmed)).isTrue();

    }
}
