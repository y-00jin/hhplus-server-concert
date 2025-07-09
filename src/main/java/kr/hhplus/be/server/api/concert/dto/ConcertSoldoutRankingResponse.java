package kr.hhplus.be.server.api.concert.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ConcertSoldoutRankingResponse {
    private Long scheduleId;
    private LocalDate concertDate;
    private int ranking;
}
