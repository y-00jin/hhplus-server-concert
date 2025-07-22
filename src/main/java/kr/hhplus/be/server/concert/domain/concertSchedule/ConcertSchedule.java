package kr.hhplus.be.server.concert.domain.concertSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ConcertSchedule {

    private Long scheduleId;
    private LocalDate concertDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConcertSchedule(Long scheduleId, LocalDate concertDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.scheduleId = scheduleId;
        this.concertDate = concertDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ConcertSchedule create(LocalDate concertDate) {
        LocalDateTime now = LocalDateTime.now();
        return new ConcertSchedule(null, concertDate, now, now);
    }

    public void assignId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
