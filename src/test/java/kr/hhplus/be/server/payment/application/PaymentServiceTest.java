package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.repository.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.model.SeatReservation;
import kr.hhplus.be.server.reservation.domain.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import kr.hhplus.be.server.user.repository.UserBalanceRepository;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {


    PaymentRepository paymentRepository = mock(PaymentRepository.class);
    SeatReservationRepository seatReservationRepository = mock(SeatReservationRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    UserBalanceRepository userBalanceRepository = mock(UserBalanceRepository.class);
    SeatRepository seatRepository = mock(SeatRepository.class);

    PaymentService paymentService;

    Long userId = 1L;
    Long reservationId = 10L;
    Long seatId = 100L;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                seatReservationRepository,
                userRepository,
                userBalanceRepository,
                seatRepository
        );
    }

    @Test
    void 결제_정상_성공() {
        // given
        long price = 50000L;
        long currentBalance = 60000L;

        User user = User.builder().userId(userId).build();
        Seat seat = Seat.builder().seatId(seatId).price((int)price).status(SeatStatus.TEMP_RESERVED).build();
        SeatReservation seatReservation = new SeatReservation(
                reservationId, userId, seatId,
                ReservationStatus.TEMP_RESERVED,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)).thenReturn(Optional.of(
                UserBalance.builder().currentBalance(currentBalance).build()
        ));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.assignId(99L);
            return p;
        });

        // when
        Payment payment = paymentService.payment(userId, reservationId);

        // then
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getReservationId()).isEqualTo(reservationId);
        assertThat(payment.getAmount()).isEqualTo(price);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // 좌석/예약 상태까지 검증
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.CONFIRMED);
        assertThat(seatReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void 결제_실패_잔액_부족() {
        // given
        long price = 70000L;
        long currentBalance = 5000L;

        User user = User.builder().userId(userId).build();
        Seat seat = Seat.builder().seatId(seatId).price((int)price).status(SeatStatus.TEMP_RESERVED).build();
        SeatReservation seatReservation = new SeatReservation(
                reservationId, userId, seatId,
                ReservationStatus.TEMP_RESERVED,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)).thenReturn(Optional.of(UserBalance.builder().currentBalance(currentBalance).build()));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        // when & then
        //assertThatThrownBy(() -> paymentService.payment(userId, reservationId)).isInstanceOf(ApiException.class);

        // when
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
    }

    @Test
    void 결제_실패_없는_예약() {
        // given
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    @Test
    void 결제_실패_이미_확정된_예약() {
        // given
        SeatReservation seatReservation = new SeatReservation(
                reservationId, userId, seatId,
                ReservationStatus.CONFIRMED,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));

        // when & then
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
    }
}
