package kr.hhplus.be.server.concert.dto.response;

import kr.hhplus.be.server.concert.domain.seat.SeatStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatResponse {
    private Long seatId;
    private Long scheduleId;
    private int seatNumber;
    private int price;
    private SeatStatus status;
}