package kr.hhplus.be.server.domain.reservation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class SeatReservationTest {

    @Test
    void 임시_예약_생성() {
        // given
        Long userId = 1L;
        Long seatId = 10L;
        int timeoutMinutes = 5;

        // when
        SeatReservation reservation = SeatReservation.createTemporary(userId, seatId, timeoutMinutes);

        // then
        assertThat(reservation.getReservationId()).isNull();
        assertThat(reservation.getUserId()).isEqualTo(userId);
        assertThat(reservation.getSeatId()).isEqualTo(seatId);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.TEMP_RESERVED);
        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(reservation.getUpdatedAt()).isNotNull();
        assertThat(reservation.getExpiredAt())
                .isAfterOrEqualTo(LocalDateTime.now().plusMinutes(timeoutMinutes - 1))
                .isBefore(LocalDateTime.now().plusMinutes(timeoutMinutes + 1)); // 시간 오차 허용
    }

    @Test
    void 예약_확정_성공() {
        // given
        SeatReservation reservation = SeatReservation.createTemporary(1L, 10L, 5);

        // when
        reservation.confirmReservation();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getExpiredAt()).isNull();
        assertThat(reservation.getUpdatedAt()).isNotNull();
    }

    @Test
    void 예약_만료_성공() {
        // given
        SeatReservation reservation = SeatReservation.createTemporary(1L, 10L, 5);

        // when
        reservation.expireReservation();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("생성자 정상 동작")
    void 생성자_정상_생성() {
        Long reservationId = 1L;
        Long userId = 1L;
        Long seatId = 1L;
        ReservationStatus status = ReservationStatus.TEMP_RESERVED;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(5);
        LocalDateTime updatedAt = now;

        // when
        SeatReservation reservation = new SeatReservation(reservationId, userId, seatId, status, expiredAt, now, updatedAt, 0L);

        // then
        assertThat(reservation.getReservationId()).isEqualTo(reservationId);
        assertThat(reservation.getUserId()).isEqualTo(userId);
        assertThat(reservation.getSeatId()).isEqualTo(seatId);
        assertThat(reservation.getStatus()).isEqualTo(status);
        assertThat(reservation.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(reservation.getCreatedAt()).isEqualTo(now);
        assertThat(reservation.getUpdatedAt()).isEqualTo(updatedAt);
    }
}