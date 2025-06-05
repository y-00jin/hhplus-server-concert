package kr.hhplus.be.server.reservation.dto;

import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeatReservationResponse {
    private Long reservationId;
    private Long userId;
    private Long seatId;
    private ReservationStatus status;
    private LocalDateTime expiredAt;
}