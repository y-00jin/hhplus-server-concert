package kr.hhplus.be.server.domain.concert;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.seat.Seat;
import kr.hhplus.be.server.concert.domain.seat.SeatStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatTest {

    @Test
    void 생성자_정상_생성() {
        // given
        Long seatId = 1L;
        Long scheduleId = 2L;
        int seatNumber = 10;
        int price = 35000;
        SeatStatus status = SeatStatus.FREE;
        LocalDateTime now = LocalDateTime.now();

        // when
        Seat seat = new Seat(seatId, scheduleId, seatNumber, price, status, now, now);

        // then
        assertThat(seat.getSeatId()).isEqualTo(seatId);
        assertThat(seat.getScheduleId()).isEqualTo(scheduleId);
        assertThat(seat.getSeatNumber()).isEqualTo(seatNumber);
        assertThat(seat.getPrice()).isEqualTo(price);
        assertThat(seat.getStatus()).isEqualTo(status);
        assertThat(seat.getCreatedAt()).isEqualTo(now);
        assertThat(seat.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void create_팩토리_정상_생성() {
        // given
        Long scheduleId = 3L;
        int seatNumber = 5;
        int price = 40000;
        SeatStatus status = SeatStatus.FREE;

        // when
        Seat seat = Seat.create(scheduleId, seatNumber, price, status);

        // then
        assertThat(seat.getSeatId()).isNull();
        assertThat(seat.getScheduleId()).isEqualTo(scheduleId);
        assertThat(seat.getSeatNumber()).isEqualTo(seatNumber);
        assertThat(seat.getPrice()).isEqualTo(price);
        assertThat(seat.getStatus()).isEqualTo(status);
        assertThat(seat.getCreatedAt()).isNotNull();
        assertThat(seat.getUpdatedAt()).isNotNull();
    }

    @Test
    void assignId_정상동작() {
        // given
        Seat seat = Seat.create(1L, 1, 10000, SeatStatus.FREE);

        // when
        seat.assignId(99L);

        // then
        assertThat(seat.getSeatId()).isEqualTo(99L);
    }

    @Test
    void setStatus_정상_변경() {
        // given
        Seat seat = Seat.create(1L, 1, 10000, SeatStatus.FREE);

        // when
        seat.setStatus(SeatStatus.TEMP_RESERVED);

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.TEMP_RESERVED);
    }

    @Test
    void reserveTemporarily_성공() {
        // given
        Seat seat = Seat.create(1L, 2, 12000, SeatStatus.FREE);

        // when
        seat.reserveTemporarily();

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.TEMP_RESERVED);
        assertThat(seat.getUpdatedAt()).isNotNull();
    }

    @Test
    void reserveTemporarily_실패_이미예약() {
        // given
        Seat seat = Seat.create(1L, 2, 12000, SeatStatus.TEMP_RESERVED);

        // when & then
        assertThatThrownBy(seat::reserveTemporarily)
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void releaseAssignment_정상동작() {
        // given
        Seat seat = Seat.create(1L, 2, 15000, SeatStatus.TEMP_RESERVED);

        // when
        seat.releaseAssignment();

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.FREE);
        assertThat(seat.getUpdatedAt()).isNotNull();
    }

    @Test
    void confirmReservation_정상동작() {
        // given
        Seat seat = Seat.create(1L, 2, 17000, SeatStatus.TEMP_RESERVED);

        // when
        seat.confirmReservation();

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.CONFIRMED);
        assertThat(seat.getUpdatedAt()).isNotNull();
    }

    @Test
    void isAvailable_FREE상태_항상_true() {
        // given
        Seat seat = Seat.create(1L, 3, 11000, SeatStatus.FREE);

        // when
        boolean result = seat.isAvailable(10);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isAvailable_TEMP_RESERVED_만료상태_true() {
        // given: 20분 전 임시예약
        LocalDateTime old = LocalDateTime.now().minusMinutes(20);
        Seat seat = new Seat(10L, 1L, 4, 12000, SeatStatus.TEMP_RESERVED, old, old);

        // when
        boolean result = seat.isAvailable(10);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isAvailable_TEMP_RESERVED_미만료상태_false() {
        // given: 2분 전 임시예약
        LocalDateTime recent = LocalDateTime.now().minusMinutes(2);
        Seat seat = new Seat(10L, 1L, 5, 12000, SeatStatus.TEMP_RESERVED, recent, recent);

        // when
        boolean result = seat.isAvailable(10);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isExpired_TEMP_RESERVED_만료상태_true() {
        // given: 15분 전 임시예약
        LocalDateTime old = LocalDateTime.now().minusMinutes(15);
        Seat seat = new Seat(1L, 1L, 6, 13000, SeatStatus.TEMP_RESERVED, old, old);

        // when
        boolean result = seat.isExpired(10);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isExpired_TEMP_RESERVED_미만료상태_false() {
        // given: 5분 전 임시예약
        LocalDateTime recent = LocalDateTime.now().minusMinutes(5);
        Seat seat = new Seat(2L, 2L, 7, 14000, SeatStatus.TEMP_RESERVED, recent, recent);

        // when
        boolean result = seat.isExpired(10);

        // then
        assertThat(result).isFalse();
    }
}