package kr.hhplus.be.server.domain.concert;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;

import java.time.LocalDateTime;

public class Seat {

    private Long seatId;
    private Long scheduleId;
    private int seatNumber;
    private int price;
    private SeatStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public Seat(Long seatId, Long scheduleId, int seatNumber, int price, SeatStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.seatId = seatId;
        this.scheduleId = scheduleId;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Seat create(Long scheduleId, int seatNumber, int price, SeatStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Seat(null, scheduleId, seatNumber, price, status, now, now, 0L);
    }

    public void assignId(Long seatId) {
        this.seatId = seatId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public int getPrice() {
        return price;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public Long getVersion() { return version; }

    /**
     * # Method설명 : 상태값 변경
     * # MethodName : setStatus
     **/
    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    /**
     * # Method설명 : 임시 예약
     * # MethodName : reserveTemporarily
     **/
    public void reserveTemporarily() {
        if (this.status != SeatStatus.FREE)
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "선택한 좌석("+ seatNumber +")은 이미 예약된 좌석입니다.");

        this.status = SeatStatus.TEMP_RESERVED;
        this.updatedAt = LocalDateTime.now();  // 임시예약 시각 기록
    }

    /**
     * # Method설명 : 임시예약 만료여부 판단 (updatedAt + timeout)
     * # MethodName : isExpired
     **/
    public boolean isExpired(int timeoutMinutes) {
        return status == SeatStatus.TEMP_RESERVED
                && updatedAt != null
                && updatedAt.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * # Method설명 : 예약 가능 여부 (FREE 또는 만료된 임시예약)
     * # MethodName : isAvailable
     **/
    public boolean isAvailable(int timeoutMinutes) {
        return status == SeatStatus.FREE
                || (status == SeatStatus.TEMP_RESERVED && isExpired(timeoutMinutes));
    }

    /**
     * # Method설명 : 임시예약 해제 (만료되었을 때 사용)
     * # MethodName : releaseAssignment
     **/
    public void releaseAssignment() {
        this.status = SeatStatus.FREE;
        this.updatedAt = LocalDateTime.now(); // 해제시각 갱신
    }

    /**
     * # Method설명 : 예약 확정 처리
     * # MethodName : confirmReservation
     **/
    public void confirmReservation() {
        this.status = SeatStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }
}
