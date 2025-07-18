package kr.hhplus.be.server.domain.concert;

import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertSchedule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ConcertScheduleTest {

    @Test
    void 생성자_정상_생성() {
        // given
        Long scheduleId = 1L;
        LocalDate concertDate = LocalDate.of(2025, 8, 7);
        LocalDateTime createdAt = LocalDateTime.of(2025, 8, 1, 13, 30);
        LocalDateTime updatedAt = createdAt.plusDays(1);

        // when
        ConcertSchedule schedule = new ConcertSchedule(scheduleId, concertDate, createdAt, updatedAt);

        // then
        assertThat(schedule.getScheduleId()).isEqualTo(scheduleId);
        assertThat(schedule.getConcertDate()).isEqualTo(concertDate);
        assertThat(schedule.getCreatedAt()).isEqualTo(createdAt);
        assertThat(schedule.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void create_정적_팩토리_정상_생성() {
        // given
        LocalDate concertDate = LocalDate.of(2025, 10, 10);

        // when
        ConcertSchedule schedule = ConcertSchedule.create(concertDate);

        // then
        assertThat(schedule.getScheduleId()).isNull();
        assertThat(schedule.getConcertDate()).isEqualTo(concertDate);
        assertThat(schedule.getCreatedAt()).isNotNull();
        assertThat(schedule.getUpdatedAt()).isNotNull();
        assertThat(schedule.getCreatedAt()).isEqualTo(schedule.getUpdatedAt());
    }

    @Test
    void assignId_정상_동작() {
        // given
        ConcertSchedule schedule = ConcertSchedule.create(LocalDate.of(2025, 7, 8));
        assertThat(schedule.getScheduleId()).isNull();

        // when
        schedule.assignId(10L);

        // then
        assertThat(schedule.getScheduleId()).isEqualTo(10L);
    }
}
