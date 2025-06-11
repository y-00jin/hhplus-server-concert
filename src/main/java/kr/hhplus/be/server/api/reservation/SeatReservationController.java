package kr.hhplus.be.server.api.reservation;

import kr.hhplus.be.server.api.reservation.dto.SeatReservationRequest;
import kr.hhplus.be.server.api.reservation.dto.SeatReservationResponse;
import kr.hhplus.be.server.application.reservation.ReserveSeatService;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reservations/seats")
public class SeatReservationController {

    private final ReserveSeatService reserveSeatService;

    @PostMapping
    public ResponseEntity<SeatReservationResponse> reserveSeat(@RequestBody SeatReservationRequest req) {
        SeatReservation reservation = reserveSeatService.reserveSeat(req.getUserId(), req.getConcertDate(), req.getSeatNumber());
        SeatReservationResponse result = SeatReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .userId(reservation.getUserId())
                .seatId(reservation.getSeatId())
                .status(reservation.getStatus().name())
                .expiredAt(reservation.getExpiredAt())
                .build();

        return ResponseEntity.ok(result);
    }
}
