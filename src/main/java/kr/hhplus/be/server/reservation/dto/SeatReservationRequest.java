package kr.hhplus.be.server.reservation.dto;

import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SeatReservationRequest {

    private LocalDate concertDate;  // 콘서트 날짜
    private int seatNumber; // 좌석 번호

}