package kr.hhplus.be.server.concert.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ConcertSoldoutEvent {

    private final Long scheduleId;

}
