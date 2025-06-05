package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.reservation.dto.SeatReservationRequest;
import kr.hhplus.be.server.reservation.dto.SeatReservationResponse;

public interface SeatReservationService {

    SeatReservationResponse reserveSeat(Long userId, SeatReservationRequest request);

}
