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

    public Seat(Long seatId, Long scheduleId, int seatNumber, int price, SeatStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.seatId = seatId;
        this.scheduleId = scheduleId;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Seat create(Long scheduleId, int seatNumber, int price, SeatStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Seat(null, scheduleId, seatNumber, price, status, now, now);
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
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 좌석은 이미 예약된 좌석입니다.");

        this.status = SeatStatus.TEMP_RESERVED;
    }
}
