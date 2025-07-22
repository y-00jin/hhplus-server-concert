package kr.hhplus.be.server.concert.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ConcertScheduleResponse {
    private Long scheduleId;
    private LocalDate concertDate;
}