package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.dto.SeatReservationRequest;
import kr.hhplus.be.server.reservation.dto.SeatReservationResponse;
import kr.hhplus.be.server.reservation.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class SeatReservationServiceImpl implements SeatReservationService {

    private final SeatReservationRepository seatReservationRepository;
    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;


    @Override
    public SeatReservationResponse reserveSeat(Long userId, SeatReservationRequest request) {

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다."));

        LocalDate concertDate = request.getConcertDate();   // 콘서트 일정
        int seatNumber = request.getSeatNumber();           // 좌석 번호

        // 콘서트 일정 조회
        ConcertSchedule concertSchedule = scheduleRepository.findByConcertDate(concertDate)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 날짜에 해당하는 콘서트가 존재하지 않습니다."));

        // 좌석 조회 (예약 가능한 상태의 좌석만 조회)
        Seat seat = seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(concertSchedule.getScheduleId(), seatNumber, SeatStatus.FREE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 좌석은 존재하지 않거나 이미 예약된 좌석입니다."));

        // 좌석 상태 임시 예약으로 변경
        seat.setStatus(SeatStatus.TEMP_RESERVED);

        SeatReservation seatReservation = SeatReservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.TEMP_RESERVED)
                .expiredAt(LocalDateTime.now().plusMinutes(5))    // 임시 예약 5분
                .build();
        SeatReservation saved = seatReservationRepository.save(seatReservation);
        return saved.toResponse();
    }
}
