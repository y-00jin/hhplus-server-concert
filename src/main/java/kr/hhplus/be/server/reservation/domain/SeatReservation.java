package kr.hhplus.be.server.reservation.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.dto.SeatReservationResponse;
import kr.hhplus.be.server.user.domain.User;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "expired_at", columnDefinition = "DATETIME")
    private LocalDateTime expiredAt;

    @Column(name = "created_at", columnDefinition = "DATETIME", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME", insertable = false, updatable = false)
    private LocalDateTime updatedAt;


    public SeatReservationResponse toResponse(){
        return SeatReservationResponse.builder()
                .reservationId(this.reservationId)
                .userId(this.user.getUserId())
                .seatId(this.seat.getSeatId())
                .status(this.status)
                .expiredAt(this.expiredAt)
                .build();
    }

}
