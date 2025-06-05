package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.dto.SeatReservationRequest;
import kr.hhplus.be.server.reservation.dto.SeatReservationResponse;
import kr.hhplus.be.server.reservation.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SeatReservationServiceTest {
    @InjectMocks
    SeatReservationServiceImpl seatReservationService;

    @Mock
    SeatReservationRepository seatReservationRepository;
    @Mock
    ConcertScheduleRepository scheduleRepository;
    @Mock
    SeatRepository seatRepository;
    @Mock
    UserRepository userRepository;

    private Long userId = 1L;
    private Long scheduleId = 1L;
    private int seatNumber = 12;
    private LocalDate concertDate = LocalDate.of(2025, 6, 7);

    @Test
    void 좌석_임시예약_성공() {
        // given
        User user = mock(User.class);
        ConcertSchedule schedule = mock(ConcertSchedule.class);
        Seat seat = mock(Seat.class);

        SeatReservationRequest request = SeatReservationRequest.builder()
                .concertDate(concertDate)
                .seatNumber(seatNumber)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));    // 사용자 조회
        when(user.getUserId()).thenReturn(userId);
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.of(schedule));  // 날짜로 콘서트 조회
        when(schedule.getScheduleId()).thenReturn(scheduleId);
        when(seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(scheduleId, seatNumber, SeatStatus.FREE)).thenReturn(Optional.of(seat)); // 일정id, 좌석번호, 예약가능으로 좌석 조회
        doNothing().when(seat).setStatus(SeatStatus.TEMP_RESERVED); // 임시 예약 상태로 변경했는지 검증
        when(seatReservationRepository.save(any(SeatReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        SeatReservationResponse response = seatReservationService.reserveSeat(userId, request);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.TEMP_RESERVED);
        assertThat(response.getExpiredAt()).isNotNull();

        verify(userRepository).findById(userId);
        verify(scheduleRepository).findByConcertDate(concertDate);
        verify(seatRepository).findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(scheduleId, seatNumber, SeatStatus.FREE);
        verify(seat).setStatus(SeatStatus.TEMP_RESERVED);
        verify(seatReservationRepository).save(any(SeatReservation.class));
    }

    @Test
    void 사용자_없으면_예외_발생() {
        // given
        SeatReservationRequest request = SeatReservationRequest.builder()
                .concertDate(concertDate)
                .seatNumber(seatNumber)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        ApiException ex = catchThrowableOfType(() -> seatReservationService.reserveSeat(userId, request), ApiException.class);

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(userRepository).findById(userId);
        verifyNoInteractions(scheduleRepository, seatRepository, seatReservationRepository);
    }

    @Test
    void 일정_없으면_예외_발생() {
        // given
        User user = mock(User.class);

        SeatReservationRequest request = SeatReservationRequest.builder()
                .concertDate(concertDate)
                .seatNumber(seatNumber)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.empty());

        // when
        ApiException ex = catchThrowableOfType(() -> seatReservationService.reserveSeat(userId, request), ApiException.class);

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(scheduleRepository).findByConcertDate(concertDate);
        verifyNoInteractions(seatRepository, seatReservationRepository);
    }

    @Test
    void 좌석_없으면_예외_발생() {
        // given
        User user = mock(User.class);
        ConcertSchedule schedule = mock(ConcertSchedule.class);

        SeatReservationRequest request = SeatReservationRequest.builder()
                .concertDate(concertDate)
                .seatNumber(seatNumber)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.findByConcertDate(concertDate)).thenReturn(Optional.of(schedule));
        when(schedule.getScheduleId()).thenReturn(scheduleId);
        when(seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(scheduleId, seatNumber, SeatStatus.FREE)).thenReturn(Optional.empty());

        // when
        ApiException ex = catchThrowableOfType(() -> seatReservationService.reserveSeat(userId, request), ApiException.class);

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(scheduleRepository).findByConcertDate(concertDate);
        verify(seatRepository).findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(scheduleId, seatNumber, SeatStatus.FREE);
        verifyNoInteractions(seatReservationRepository);
    }

}