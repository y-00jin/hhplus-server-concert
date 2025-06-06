package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.dto.SeatReservationResponse;
import kr.hhplus.be.server.user.domain.User;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private SeatReservation seatReservation;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", columnDefinition = "DATETIME", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public PaymentResponse toResponse(){
        return PaymentResponse.builder()
                .paymentId(this.paymentId)
                .userId(this.user.getUserId())
                .reservationId(this.seatReservation.getReservationId())
                .amount(this.amount)
                .status(this.status)
                .build();
    }

}
