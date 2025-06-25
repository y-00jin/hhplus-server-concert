package kr.hhplus.be.server.application.reservation;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReserveSeatServiceTest {

    @InjectMocks
    ReserveSeatService reserveSeatService;

    @Mock
    SeatReservationRepository reservationRepository;
    @Mock
    ConcertScheduleRepository scheduleRepository;
    @Mock
    SeatRepository seatRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    QueueTokenRepository queueTokenRepository;
    @Mock
    DistributedLockRepository distributedLockRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("좌석 예약 성공 - 정상 플로우")
    void reserveSeat_Success() {
        // given
        Long userId = 1L;
        LocalDate concertDate = LocalDate.now().plusDays(1);
        int seatNumber = 1;

        User user = mock(User.class);
        ConcertSchedule schedule = mock(ConcertSchedule.class);
        QueueToken queueToken = mock(QueueToken.class);
        Seat seat = mock(Seat.class);
        SeatReservation reservation = mock(SeatReservation.class);

        // 1. 사용자/일정/토큰 등 기존 mock
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.of(schedule));
        when(schedule.getScheduleId()).thenReturn(100L);
        when(schedule.getConcertDate()).thenReturn(concertDate);

        when(queueTokenRepository.findTokenIdByUserIdAndScheduleId(eq(userId), eq(100L))).thenReturn(Optional.of("queue-token-id"));
        when(queueTokenRepository.findQueueTokenByTokenId("queue-token-id")).thenReturn(Optional.of(queueToken));
        when(queueToken.isExpired()).thenReturn(false);
        when(queueToken.isActive()).thenReturn(true);

        // 2. seatId 먼저 조회 (추가)
        when(seatRepository.findSeatIdByScheduleIdAndSeatNumber(100L, seatNumber)).thenReturn(123L);

        // 3. 분산락 획득
        when(distributedLockRepository.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);

        // 4. 락 획득 후 seat 다시 조회
        when(seatRepository.findById(123L)).thenReturn(Optional.of(seat));

        // 5. seat 예약가능 mock
        when(seat.getStatus()).thenReturn(SeatStatus.TEMP_RESERVED);
        when(seat.isExpired(anyInt())).thenReturn(false);
        when(seat.isAvailable(anyInt())).thenReturn(true);
        doNothing().when(seat).reserveTemporarily();
        when(seat.getSeatId()).thenReturn(123L);

        // 6. 예약 저장
        when(reservationRepository.save(any())).thenReturn(reservation);

        // when
        SeatReservation result = reserveSeatService.reserveSeat(userId, concertDate, seatNumber);

        // then
        assertThat(result).isNotNull();
        verify(seatRepository, times(1)).save(seat);
        verify(reservationRepository, times(1)).save(any(SeatReservation.class));
        verify(distributedLockRepository, times(1)).unlock(anyString(), anyString());

        // seatId 조회, seat 조회 2번 모두 검증
        verify(seatRepository, times(1)).findSeatIdByScheduleIdAndSeatNumber(100L, seatNumber);
        verify(seatRepository, times(1)).findById(123L);
    }

    @Test
    @DisplayName("좌석 예약 실패 - 사용자 없음")
    void reserveSeat_userNotFound() {
        // given
        Long userId = 2L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.reserveSeat(userId, LocalDate.now().plusDays(1), 5))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("좌석 예약 실패 - 일정 없음")
    void reserveSeat_scheduleNotFound() {
        // given
        Long userId = 1L;
        LocalDate concertDate = LocalDate.now().plusDays(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.reserveSeat(userId, concertDate, 5))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("좌석 예약 실패 - 대기열 토큰 없음")
    void reserveSeat_noQueueToken() {
        // given
        Long userId = 1L;
        LocalDate concertDate = LocalDate.now().plusDays(1);
        ConcertSchedule schedule = mock(ConcertSchedule.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.of(schedule));
        when(schedule.getScheduleId()).thenReturn(42L);
        when(schedule.getConcertDate()).thenReturn(concertDate);

        when(queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, 42L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.reserveSeat(userId, concertDate, 7))
                .isInstanceOf(ApiException.class);
    }

}