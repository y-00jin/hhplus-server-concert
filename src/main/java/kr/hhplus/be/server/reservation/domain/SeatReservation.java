package kr.hhplus.be.server.reservation.domain;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.Objects;

public class  SeatReservation {

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
    public static SeatReservation createTemporary(Long userId, Long seatId, int timeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return create(userId, seatId, ReservationStatus.TEMP_RESERVED, now.plusMinutes(timeoutMinutes));
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

    /**
     * # Method설명 : 예약 만료 변경
     * # MethodName : expireReservation
     **/
    public void expireReservation() {
        this.status = ReservationStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * # Method설명 : 주어진 사용자ID가 이 예약의 소유자인지 확인
     * # MethodName : validateOwner
     **/
    public void validateOwner(Long userId){
        if (!Objects.equals(this.userId, userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, String.format("예약 정보(%d)는 사용자 ID(%d)의 소유가 아닙니다.", reservationId, userId));
        }
    }

    /**
     * # Method설명 : 결제 가능 상태 검증
     * # MethodName : validateAvailableToPay
     **/
    public void validateAvailableToPay() {
        if (this.status != ReservationStatus.TEMP_RESERVED || (this.expiredAt != null && this.expiredAt.isBefore(LocalDateTime.now()))) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, String.format("해당 예약(%d)은 이미 결제되었거나 결제가 불가능한 상태입니다.", this.reservationId));
        }
    }

}
