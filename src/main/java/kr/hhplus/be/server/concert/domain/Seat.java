package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.dto.SeatResponse;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ConcertSchedule concertSchedule;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "price", nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status;

    @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public SeatResponse toResponse(){
        return SeatResponse.builder()
                .seatId(this.seatId)
                .scheduleId(this.concertSchedule.getScheduleId())
                .seatNumber(this.seatNumber)
                .price(this.price)
                .status(this.status)
                .build();
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
