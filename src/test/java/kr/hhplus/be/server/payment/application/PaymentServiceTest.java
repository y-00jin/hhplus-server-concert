package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.concert.domain.seat.Seat;
import kr.hhplus.be.server.concert.domain.seat.SeatStatus;
import kr.hhplus.be.server.lock.domain.DistributedLockRepository;
import kr.hhplus.be.server.payment.application.PaymentService;
import kr.hhplus.be.server.payment.application.PaymentTransactionalService;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.user.User;
import kr.hhplus.be.server.user.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    SeatReservationRepository seatReservationRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    DistributedLockRepository distributedLockRepository;

    @Mock
    PaymentTransactionalService paymentTransactionalService;

    @InjectMocks
    PaymentService paymentService;

    Long userId = 1L;
    Long reservationId = 1L;
    Long seatId = 1L;
    Long scheduleId = 1L;
    int price = 30000;

    User user;
    Seat seat;
    SeatReservation reservation;

    @BeforeEach
    void setUp() {
        user = new User(userId, "", "test@test.com", "pw", "사용자", null, null);
        seat = new Seat(seatId, scheduleId, 1, price, SeatStatus.TEMP_RESERVED, LocalDateTime.now(), LocalDateTime.now());
        reservation = new SeatReservation(reservationId, userId, seatId, ReservationStatus.TEMP_RESERVED, LocalDateTime.now().plusMinutes(5), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void 결제_정상_진행() {
        // given
        Payment payment = Payment.create(userId, reservationId, price, PaymentStatus.SUCCESS);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));    // 사용자 조회
        when(seatReservationRepository.findSeatIdById(reservationId)).thenReturn(seatId);   // 좌석id 조회
        when(distributedLockRepository.withMultiLock(anyList(), any(), anyLong(), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    // 실제 람다 실행
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });

        when(paymentTransactionalService.doPaymentTransactional(anyLong(), anyLong(), anyLong())).thenReturn(payment);

        // when
        Payment result = paymentService.payment(userId, reservationId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getReservationId()).isEqualTo(reservationId);
        assertThat(result.getAmount()).isEqualTo(price);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

    }

    @Test
    void 존재하지_않는_사용자는_예외() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOf(ApiException.class);
    }
}