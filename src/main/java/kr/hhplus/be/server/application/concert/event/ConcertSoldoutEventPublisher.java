package kr.hhplus.be.server.application.concert.event;

import kr.hhplus.be.server.domain.concert.event.ConcertSoldoutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertSoldoutEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(ConcertSoldoutEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

}
