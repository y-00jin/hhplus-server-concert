package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.dto.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.dto.SeatResponse;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {

    @InjectMocks
    ConcertServiceImpl concertService;

    @Mock
    ConcertScheduleRepository scheduleRepository;

    @Mock
    SeatRepository seatRepository;

    /**
     * # Method설명 : 콘서트 일정이 오늘 이후에 진행되는 일정만 조회
     * # MethodName : 예약_가능_콘서트_일정_목록_조회_성공
     **/
    @Test
    void 예약_가능_콘서트_일정_목록_조회_성공(){
        // given
        ConcertSchedule mockSchedule = mock(ConcertSchedule.class);
        ConcertScheduleResponse mockScheduleResponse = mock(ConcertScheduleResponse.class);

        when(scheduleRepository.findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(any())).thenReturn(List.of(mockSchedule));    // 일정 조회 시 List mockSchedule 반환
        when(mockSchedule.toResponse()).thenReturn(mockScheduleResponse);

        // when
        List<ConcertScheduleResponse> result = concertService.getAvailableSchedules();  // 예약 가능 콘서트 일정 목록 조회

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(mockScheduleResponse);

        verify(scheduleRepository, times(1)).findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(any());
        verify(mockSchedule, times(1)).toResponse();
    }


    @Test
    void 특정_날짜로_예약_가능_좌석_목록_조회_성공() {

        // given
        LocalDate date = LocalDate.of(2025, 6, 5);
        Long scheduleId = 1L;
        ConcertSchedule mockSchedule = mock(ConcertSchedule.class);
        Seat mockSeat = mock(Seat.class);
        SeatResponse mockSeatResponse = mock(SeatResponse.class);

        when(scheduleRepository.findByConcertDate(date)).thenReturn(Optional.of(mockSchedule)); // 날짜로 일정 조회 시 mockSchedule 반환
        when(mockSchedule.getScheduleId()).thenReturn(scheduleId);                             // 일정ID 반환
        when(seatRepository.findAllByConcertSchedule_ScheduleIdAndStatus(scheduleId, SeatStatus.FREE)).thenReturn(List.of(mockSeat));                                                // 좌석 조회
        when(mockSeat.toResponse()).thenReturn(mockSeatResponse);                              // 좌석 응답 변환

        // when
        List<SeatResponse> result = concertService.getAvailableSeatsByDate(date);   // 콘서트 일정의 좌석 목록 조회

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(mockSeatResponse);
        verify(scheduleRepository, times(1)).findByConcertDate(date);
        verify(mockSchedule, times(1)).getScheduleId();
        verify(seatRepository, times(1)).findAllByConcertSchedule_ScheduleIdAndStatus(scheduleId, SeatStatus.FREE);
        verify(mockSeat, times(1)).toResponse();
    }


    @Test
    void 특정_날짜로_예약_가능_좌석_목록_조회_실패_일정없음() {
        // given
        LocalDate date = LocalDate.of(2025, 6, 5); // 존재하지 않는 날짜
        when(scheduleRepository.findByConcertDate(date)).thenReturn(Optional.empty());

        // when & then
        ApiException ex = catchThrowableOfType(() -> concertService.getAvailableSeatsByDate(date), ApiException.class);

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(scheduleRepository, times(1)).findByConcertDate(date);
        verifyNoInteractions(seatRepository);
    }

}