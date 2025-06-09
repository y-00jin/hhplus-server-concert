package kr.hhplus.be.server.reservation.domain.model;

import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;

import java.time.LocalDateTime;

public class SeatReservation {

    private Long reservationId;
    private Long userId;
    private Long seatId;
    private ReservationStatus status;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SeatReservation(Long reservationId, Long userId, Long seatId, ReservationStatus status, LocalDateTime expiredAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.seatId = seatId;
        this.status = status;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getReservationId() { return reservationId; }
    public Long getUserId() { return userId; }
    public Long getSeatId() { return seatId; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void assignId(Long reservationId) { this.reservationId = reservationId; }


    public static SeatReservation create(Long userId, Long seatId, ReservationStatus status, LocalDateTime expiredAt) {
        LocalDateTime now = LocalDateTime.now();
        return new SeatReservation(null, userId, seatId, status, expiredAt, now, now);
    }

    /**
     * # Method설명 : 임시 예약 생성
     * # MethodName : createTemporary
     **/
    public static SeatReservation createTemporary(Long userId, Long seatId) {
        LocalDateTime now = LocalDateTime.now();
        return new SeatReservation(null, userId, seatId, ReservationStatus.TEMP_RESERVED, now.plusMinutes(5), now, now);
    }

    /**
     * # Method설명 : 예약 확정 변경
     * # MethodName : confirmReservation
     **/
    public void confirmReservation(){
        this.status = ReservationStatus.CONFIRMED;
        this.expiredAt = null;
        this.updatedAt = LocalDateTime.now();
    }
}
