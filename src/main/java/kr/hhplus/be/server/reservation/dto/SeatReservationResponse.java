package kr.hhplus.be.server.reservation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeatReservationResponse {
    private Long reservationId;
    private Long userId;
    private Long seatId;
    private String status;
    private LocalDateTime expiredAt;
}
