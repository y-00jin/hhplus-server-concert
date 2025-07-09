package kr.hhplus.be.server.application.concert;

import kr.hhplus.be.server.domain.concert.SoldoutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SoldoutEventListener {

    private final ConcertService concertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSoldoutEvent(SoldoutEvent event) {
        concertService.checkAndRegisterSoldoutRanking(event.getScheduleId());
    }
}
