package kr.hhplus.be.server.application.batch;

import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.domain.concert.ConcertSchedule;
import kr.hhplus.be.server.domain.concert.ConcertScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QueueBatch {

    private final QueueService queueService;
    private final ConcertScheduleRepository scheduleRepository;

    /**
     * # Method설명 : 콘서트 일정에 대해, 각 대기열에서 입장 가능한 인원을 활성(ACTIVE) 상태로 자동 변경
     * # MethodName : promoteAllQueues
     **/
    @Scheduled(fixedDelay = 5000)
    public void promoteAllQueues() {
        List<ConcertSchedule> schedules = scheduleRepository.findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate.now());
        for (ConcertSchedule schedule : schedules) {
            queueService.promoteWaitingToActive(schedule.getScheduleId());
        }
    }

}
