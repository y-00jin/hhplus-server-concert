package kr.hhplus.be.server.api.concert.dto;

import kr.hhplus.be.server.domain.concert.SeatStatus;
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