package kr.hhplus.be.server.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationRequest {
    private Long userId;
    private LocalDate concertDate;
    private int seatNumber;
}
