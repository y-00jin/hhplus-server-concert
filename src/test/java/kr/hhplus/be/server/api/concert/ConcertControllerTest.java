package kr.hhplus.be.server.api.concert;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.concert.api.ConcertController;
import kr.hhplus.be.server.concert.application.ConcertService;
import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.seat.Seat;
import kr.hhplus.be.server.concert.domain.seat.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ConcertController.class)
public class ConcertControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ConcertService concertService;

    @Autowired
    ObjectMapper objectMapper;


    @Test
    @DisplayName("오늘 이후의 예약 가능 콘서트 일정 목록 조회 성공")
    void getAvailableSchedules() throws Exception {
        // given
        ConcertSchedule s1 = new ConcertSchedule(1L, LocalDate.of(2025, 7, 1), null, null);
        ConcertSchedule s2 = new ConcertSchedule(2L, LocalDate.of(2025, 7, 2), null, null);
        List<ConcertSchedule> schedules = Arrays.asList(s1, s2);

        when(concertService.getAvailableSchedules()).thenReturn(schedules);

        // when & then
        mockMvc.perform(get("/api/v1/concerts/schedules")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].scheduleId").value(1))
                .andExpect(jsonPath("$[0].concertDate").value("2025-07-01"));
    }


    @Test
    @DisplayName("특정 날짜 예약 가능 좌석 목록 조회 성공")
    void getAvailableSeatsByDate() throws Exception {

        LocalDate date = LocalDate.of(2025, 7, 1);
        Seat seat1 = new Seat(1L, 1L, 1, 10000, SeatStatus.FREE, null, null);
        Seat seat2 = new Seat(2L, 1L, 2, 10000, SeatStatus.FREE, null, null);
        List<Seat> seatList = Arrays.asList(seat1, seat2);

        when(concertService.getAvailableSeatsByDate(date)).thenReturn(seatList);

        mockMvc.perform(get("/api/v1/concerts/schedules/{date}/seats", date)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].seatNumber").value(1))
                .andExpect(jsonPath("$[1].status").value("FREE"));
    }

}
