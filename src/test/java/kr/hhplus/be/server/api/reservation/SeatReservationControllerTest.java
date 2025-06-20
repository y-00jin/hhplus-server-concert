package kr.hhplus.be.server.api.reservation;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.reservation.dto.SeatReservationRequest;
import kr.hhplus.be.server.application.reservation.ReserveSeatService;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatReservationController.class)
class SeatReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReserveSeatService reserveSeatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("좌석 예약 성공")
    void reserveSeat_success() throws Exception {
        // given
        SeatReservationRequest req = SeatReservationRequest.builder()
                .userId(1L)
                .concertDate(LocalDate.of(2025, 7, 2))
                .seatNumber(10)
                .build();

        SeatReservation reservation = new SeatReservation(
                11L,
                1L,
                33L,
                ReservationStatus.TEMP_RESERVED,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(reserveSeatService.reserveSeat(
                eq(req.getUserId()),
                eq(req.getConcertDate()),
                eq(req.getSeatNumber())
        )).thenReturn(reservation);

        // when & then
        mockMvc.perform(post("/api/v1/reservations/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(11L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.seatId").value(33L))
                .andExpect(jsonPath("$.status").value("TEMP_RESERVED"))
                .andExpect(jsonPath("$.expiredAt").exists());
    }
}