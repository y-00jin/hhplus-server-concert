package kr.hhplus.be.server.domain.concert;

public class SoldoutEvent {

    private final Long scheduleId;

    public SoldoutEvent(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }
}
