package kr.hhplus.be.server.concert.application.event;

import kr.hhplus.be.server.concert.application.ConcertService;
import kr.hhplus.be.server.concert.domain.event.ConcertSoldoutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ConcertSoldoutEventListener {

    private final ConcertService concertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSoldoutEvent(ConcertSoldoutEvent event) {
        concertService.checkAndRegisterSoldoutRanking(event.getScheduleId());
    }
}
