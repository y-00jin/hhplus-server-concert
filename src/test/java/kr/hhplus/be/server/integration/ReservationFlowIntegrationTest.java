package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.application.reservation.ReserveSeatService;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationFlowIntegrationTest {

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
    ReserveSeatService reserveSeatService;
    @Autowired
    PaymentService paymentService;

    private Long userId;
    private Long user2Id;
    private Long scheduleId;
    private Long seatId;
    private final LocalDate concertDate = LocalDate.of(2025, 7, 1);
    private final int seatNumber = 1;
    private LocalDateTime now;


    @BeforeEach
    void setup() {
        now = LocalDateTime.now();

        // 사용자 등록
        userId = userRepository.save(new User(null, UUID.randomUUID().toString(), "test@email.com", "pw", "사용자1", now, now)).getUserId();
        user2Id = userRepository.save(new User(null, UUID.randomUUID().toString(), "test2@email.com", "pw", "사용자2", now, now)).getUserId();

        // 콘서트 일정 및 좌석 등록
        scheduleId = scheduleRepository.save(new ConcertSchedule(null, concertDate, now, now)).getScheduleId();
        seatId = seatRepository.save(new Seat(null, scheduleId, seatNumber, 30000, SeatStatus.FREE, now, now, 0L)).getSeatId();

        // 포인트 충전
        userBalanceRepository.save(UserBalance.charge(userId, 50000L, 0L));
        userBalanceRepository.save(UserBalance.charge(user2Id, 50000L, 0L));

        // 대기열 토큰 발급
        queueTokenRepository.save(new QueueToken(UUID.randomUUID().toString(), userId, scheduleId, 0, QueueStatus.ACTIVE, now, now.plusMinutes(5)));
        queueTokenRepository.save(new QueueToken(UUID.randomUUID().toString(), user2Id, scheduleId, 0, QueueStatus.ACTIVE, now, now.plusMinutes(5)));
    }

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAllForTest();
        seatReservationRepository.deleteAllForTest();
        userBalanceRepository.deleteAllForTest();
        seatRepository.deleteAllForTest();
        scheduleRepository.deleteAllForTest();
        userRepository.deleteAllForTest();

    }

    @Test
    void 유저가_좌석을_예약하고_결제까지_완료() {
        SeatReservation reservation = reserveSeatService.reserveSeat(userId, concertDate, seatNumber);
        Payment payment = paymentService.payment(userId, reservation.getReservationId());

        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(seatRepository.findById(seatId).get().getStatus()).isEqualTo(SeatStatus.CONFIRMED);
    }

    @Test
    void 예약이_만료되면_좌석을_다시_예약할_수_있다() {
        SeatReservation reservation = reserveSeatService.reserveSeat(userId, concertDate, seatNumber);

        reservation.expireReservation();    // 예약 - 만료 처리
        seatReservationRepository.save(reservation);

        Seat seat = seatRepository.findById(seatId).get();
        seat.releaseAssignment();   // 좌석 - 만료 처리
        seatRepository.save(seat);

        SeatReservation newReservation = reserveSeatService.reserveSeat(userId, concertDate, seatNumber);
        assertThat(newReservation.getStatus()).isEqualTo(ReservationStatus.TEMP_RESERVED);
    }

    @Test
    void 동시에_예약을_시도하면_한명만_성공해야_한다() throws InterruptedException {

        // 좌석 만료 처리
        Seat seat = seatRepository.findById(seatId).get();
        seat.releaseAssignment();
        seatRepository.save(seat);

        AtomicReference<SeatReservation> result1 = new AtomicReference<>();
        AtomicReference<SeatReservation> result2 = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                result1.set(reserveSeatService.reserveSeat(userId, concertDate, seatNumber));
            } catch (Exception e) {
                e.printStackTrace();
                result1.set(null);
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                result2.set(reserveSeatService.reserveSeat(user2Id, concertDate, seatNumber));
            } catch (Exception e) {
                e.printStackTrace();
                result2.set(null);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // then
        long successCount = Stream.of(result1.get(), result2.get())
                .filter(Objects::nonNull)
                .count();
        assertThat(successCount).isEqualTo(1);
    }

}