package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import kr.hhplus.be.server.user.repository.UserBalanceRepository;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserBalanceRepository userBalanceRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;
    private Seat seat;
    private SeatReservation seatReservation;

    private Long userId = 1L;
    private Long reservationId = 1L;


    @BeforeEach
    void setUp() {
        user = User.builder().userId(userId).build();
        seat = Seat.builder().seatId(1L).price(10000).status(SeatStatus.TEMP_RESERVED).build();
        seatReservation = SeatReservation.builder()
                .reservationId(reservationId)
                .user(user)
                .seat(seat)
                .status(ReservationStatus.TEMP_RESERVED)
                .build();
    }

    @Test
    void 결제_정상_처리() {
        // given
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(UserBalance.builder().currentBalance(20000).build()));
        when(userBalanceRepository.save(any(UserBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PaymentResponse response = paymentService.payment(userId, reservationId);

        // then
        assertThat(response.getAmount()).isEqualTo(10000);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(seatReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.CONFIRMED);
    }

    @Test
    void 결제_실패_잔액부족() {
        // given
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(UserBalance.builder().currentBalance(5000).build())); // 부족

        // when & then
        ApiException ex = catchThrowableOfType(() -> paymentService.payment(userId, reservationId), ApiException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 결제_실패_이미_결제된_예약() {
        // given
        seatReservation.confirmReservation();
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));

        // when & then
        ApiException ex = catchThrowableOfType(() -> paymentService.payment(userId, reservationId), ApiException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 결제_실패_예약_정보없음() {
        // given
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = catchThrowableOfType(() -> paymentService.payment(userId, reservationId), ApiException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void 결제_실패_사용자_정보없음() {
        // given
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(seatReservation));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = catchThrowableOfType(() -> paymentService.payment(userId, reservationId), ApiException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}