package kr.hhplus.be.server.reservation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SeatReservationRequest {
    private Long userId;
    private LocalDate concertDate;
    private int seatNumber;
}
