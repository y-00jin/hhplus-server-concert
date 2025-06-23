package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private SeatReservationRepository seatReservationRepository;
    private UserRepository userRepository;
    private UserBalanceRepository userBalanceRepository;
    private SeatRepository seatRepository;
    private QueueTokenRepository queueTokenRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        seatReservationRepository = mock(SeatReservationRepository.class);
        userRepository = mock(UserRepository.class);
        userBalanceRepository = mock(UserBalanceRepository.class);
        seatRepository = mock(SeatRepository.class);
        queueTokenRepository = mock(QueueTokenRepository.class);

        paymentService = new PaymentService(
                paymentRepository,
                seatReservationRepository,
                userRepository,
                userBalanceRepository,
                seatRepository,
                queueTokenRepository
        );
    }

    @Test
    void 결제_정상_진행() {
        // given
        Long userId = 1L;
        Long reservationId = 100L;
        Long seatId = 10L;
        Long scheduleId = 5L;
        int price = 30000;
        long currentBalance = 50000L;

        User user = new User(userId, "", "test@test.com", "pw", "사용자", null, null);
        Seat seat = new Seat(seatId, scheduleId, 1, price, SeatStatus.TEMP_RESERVED, LocalDateTime.now(), LocalDateTime.now());
        SeatReservation reservation = new SeatReservation(reservationId, userId, seatId, ReservationStatus.TEMP_RESERVED, LocalDateTime.now().plusMinutes(10), LocalDateTime.now(), LocalDateTime.now());
        Payment payment = Payment.create(userId, reservationId, price, PaymentStatus.SUCCESS);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));    // 사용자 조회
        when(seatReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));  // 예약 조회
        when(seatRepository.findBySeatIdForUpdate(seatId)).thenReturn(Optional.of(seat));   // 좌석 조회
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId))    // 잔액 조회
                .thenReturn(Optional.of(new UserBalance(null, userId, 0L, UserBalanceType.CHARGE, currentBalance, null, null)));
        when(userBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, scheduleId)).thenReturn(Optional.empty());

        // when
        Payment result = paymentService.payment(userId, reservationId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getReservationId()).isEqualTo(reservationId);
        assertThat(result.getAmount()).isEqualTo(price);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(userBalanceRepository).save(any());
        verify(seatReservationRepository).save(any());
        verify(seatRepository).save(any());
        verify(paymentRepository).save(any());
    }

    @Test
    void 존재하지_않는_사용자는_예외() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> paymentService.payment(1L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    void 만료된_예약은_예외() {
        // given
        Long userId = 1L;
        Long reservationId = 100L;

        SeatReservation expiredReservation = new SeatReservation(
                reservationId, userId, 10L,
                ReservationStatus.TEMP_RESERVED,
                LocalDateTime.now().minusMinutes(1),  // 만료
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        when(seatReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(expiredReservation));

        // then
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("결제할 수 없는 예약 정보");
    }
}