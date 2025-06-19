package kr.hhplus.be.server.application.concert;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConcertServiceTest {

    ConcertScheduleRepository scheduleRepository = mock(ConcertScheduleRepository.class);
    SeatRepository seatRepository = mock(SeatRepository.class);

    ConcertService concertService;

    @BeforeEach
    void setUp() {
        concertService = new ConcertService(scheduleRepository, seatRepository);
    }

    @Test
    void 오늘이후_예약_가능_콘서트_일정_조회() {
        // given
        ConcertSchedule s1 = new ConcertSchedule(1L, LocalDate.now().plusDays(1), null, null);
        ConcertSchedule s2 = new ConcertSchedule(2L, LocalDate.now().plusDays(2), null, null);
        List<ConcertSchedule> schedules = Arrays.asList(s1, s2);

        when(scheduleRepository.findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate.now()))
                .thenReturn(schedules);

        // when
        List<ConcertSchedule> result = concertService.getAvailableSchedules();

        // then
        assertThat(result).hasSize(2).containsExactly(s1, s2);
    }

    @Test
    void 특정_날짜로_콘서트좌석_조회_성공() {
        // given
        LocalDate date = LocalDate.of(2025, 6, 17);
        Long scheduleId = 1L;

        ConcertSchedule schedule = new ConcertSchedule(scheduleId, date, null, null);
        when(scheduleRepository.findByConcertDate(date)).thenReturn(Optional.of(schedule));

        Seat seat1 = new Seat(1L, scheduleId, 1, 10000, SeatStatus.FREE, null, null, 0L);
        Seat seat2 = new Seat(2L, scheduleId, 2, 10000, SeatStatus.FREE, null, null, 0L);
        List<Seat> seatList = Arrays.asList(seat1, seat2);

        when(seatRepository.findAllByConcertSchedule_ScheduleIdAndStatus(scheduleId, SeatStatus.FREE))
                .thenReturn(seatList);

        // when
        List<Seat> result = concertService.getAvailableSeatsByDate(date);

        // then
        assertThat(result).hasSize(2).containsExactly(seat1, seat2);
    }

    @Test
    void 특정_날짜로_콘서트좌석_조회_실패_일정없음() {
        // given
        LocalDate date = LocalDate.of(2025, 6, 17);
        when(scheduleRepository.findByConcertDate(date)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> concertService.getAvailableSeatsByDate(date))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}