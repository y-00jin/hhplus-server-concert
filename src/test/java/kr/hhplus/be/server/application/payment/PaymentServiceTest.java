package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock SeatReservationRepository seatReservationRepository;
    @Mock UserRepository userRepository;
    @Mock UserBalanceRepository userBalanceRepository;
    @Mock SeatRepository seatRepository;
    @Mock QueueTokenRepository queueTokenRepository;
    @Mock DistributedLockRepository distributedLockRepository;

    @Mock ConcertSoldoutRankingRepository concertSoldoutRankingRepository;
    @Mock ConcertScheduleRepository scheduleRepository;

    @InjectMocks
    PaymentService paymentService;

    Long userId = 1L;
    Long reservationId = 1L;
    Long seatId = 1L;
    Long scheduleId = 1L;
    int price = 30000;
    long currentBalance = 50000L;

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
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));  // 예약 조회
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));   // 좌석 조회
        when(userBalanceRepository.findCurrentBalanceByUserId(userId)).thenReturn(currentBalance); // 잔액 조회
        when(userBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, scheduleId)).thenReturn(Optional.empty());

        // 매진 조건 세팅 (전체 좌석 = CONFIRMED 좌석)
        when(seatRepository.countByConcertSchedule_ScheduleId(scheduleId)).thenReturn(1);
        when(seatRepository.countByConcertSchedule_ScheduleIdAndStatus(scheduleId, SeatStatus.CONFIRMED)).thenReturn(1);

        // 아직 랭킹 등록 안 됨
        when(concertSoldoutRankingRepository.isAlreadyRanked(anyString(), eq(scheduleId))).thenReturn(false);
        // 콘서트 생성일 반환
        when(scheduleRepository.findById(scheduleId)).thenReturn(
                Optional.of(new ConcertSchedule(scheduleId, LocalDate.now().plusDays(1),  LocalDateTime.now().minusMinutes(10), LocalDateTime.now().minusMinutes(10)))
        );


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
        verify(concertSoldoutRankingRepository, times(1)).addSoldoutRanking(anyString(), eq(scheduleId), anyDouble());
    }

    @Test
    void 존재하지_않는_사용자는_예외() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 만료된_예약은_예외() {
        // given
        SeatReservation expiredReservation = new SeatReservation(
                reservationId, userId, 10L,
                ReservationStatus.TEMP_RESERVED,
                LocalDateTime.now().minusMinutes(1),  // 만료
                LocalDateTime.now().minusMinutes(6),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(distributedLockRepository.withMultiLock(anyList(), any(), anyLong(), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    // 실제 람다 실행
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        when(seatReservationRepository.findById(reservationId)).thenReturn(Optional.of(expiredReservation));

        // then
        assertThatThrownBy(() -> paymentService.payment(userId, reservationId))
                .isInstanceOf(ApiException.class);
    }





}