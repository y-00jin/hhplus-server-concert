package kr.hhplus.be.server.reservation.controller;


import kr.hhplus.be.server.concert.dto.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.dto.SeatResponse;
import kr.hhplus.be.server.concert.service.ConcertService;
import kr.hhplus.be.server.reservation.dto.SeatReservationRequest;
import kr.hhplus.be.server.reservation.dto.SeatReservationResponse;
import kr.hhplus.be.server.reservation.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reservations/seats")
public class SeatReservationController {

    private final SeatReservationService seatReservationService;

    @PostMapping
    public ResponseEntity<SeatReservationResponse> reserveSeat(@RequestParam("userId") Long userId, @RequestBody SeatReservationRequest request) {
        return ResponseEntity.ok(seatReservationService.reserveSeat(userId, request));
    }
}
