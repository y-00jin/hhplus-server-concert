package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.model.SeatReservation;
import kr.hhplus.be.server.reservation.domain.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReserveSeatServiceTest {

    SeatReservationRepository reservationRepository = mock(SeatReservationRepository.class);
    ConcertScheduleRepository scheduleRepository = mock(ConcertScheduleRepository.class);
    SeatRepository seatRepository = mock(SeatRepository.class);
    UserRepository userRepository = mock(UserRepository.class);

    ReserveSeatService reserveSeatService;

    @BeforeEach
    void setUp() {
        reserveSeatService = new ReserveSeatService(
                reservationRepository, scheduleRepository, seatRepository, userRepository
        );
    }

    @Test
    void 좌석_정상_예약_성공() {
        // given
        Long userId = 1L;
        LocalDate concertDate = LocalDate.of(2025, 6, 10);
        int seatNumber = 5;
        User user = User.builder().userId(userId).build();
        ConcertSchedule schedule = ConcertSchedule.builder().scheduleId(10L).concertDate(concertDate).build();
        Seat seat = Seat.builder().seatId(100L).seatNumber(seatNumber).status(SeatStatus.FREE).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.of(schedule));
        when(seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(schedule.getScheduleId(), seatNumber, SeatStatus.FREE))
                .thenReturn(Optional.of(seat));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SeatReservation reservation = reserveSeatService.reserveSeat(userId, concertDate, seatNumber);

        // then
        assertThat(reservation.getUserId()).isEqualTo(userId);
        assertThat(reservation.getSeatId()).isEqualTo(seat.getSeatId());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.TEMP_RESERVED);
        assertThat(reservation.getExpiredAt()).isAfter(LocalDateTime.now());
        verify(seatRepository).findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(schedule.getScheduleId(), seatNumber, SeatStatus.FREE);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.TEMP_RESERVED);
    }

    @Test
    void 존재하지_않는_유저_예외() {
        // given
        Long userId = 999L;
        LocalDate date = LocalDate.now();
        int seatNumber = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.reserveSeat(userId, date, seatNumber))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 콘서트_일정_없음_예외() {
        // given
        Long userId = 1L;
        LocalDate concertDate = LocalDate.of(2025, 6, 10);
        int seatNumber = 1;
        User user = User.builder().userId(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.reserveSeat(userId, concertDate, seatNumber))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 좌석_없음_예외() {
        // given
        Long userId = 1L;
        LocalDate concertDate = LocalDate.of(2025, 6, 10);
        int seatNumber = 1;
        User user = User.builder().userId(userId).build();
        ConcertSchedule schedule = ConcertSchedule.builder().scheduleId(11L).concertDate(concertDate).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.of(schedule));
        when(seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(schedule.getScheduleId(), seatNumber, SeatStatus.FREE))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.reserveSeat(userId, concertDate, seatNumber))
                .isInstanceOf(ApiException.class);
    }



}