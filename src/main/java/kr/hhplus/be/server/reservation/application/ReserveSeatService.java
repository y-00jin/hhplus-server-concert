package kr.hhplus.be.server.reservation.application;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.reservation.domain.model.SeatReservation;
import kr.hhplus.be.server.reservation.domain.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ReserveSeatService {   // 좌석 예약 서비스

    private final SeatReservationRepository reservationRepository;
    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    // 좌석 예약 유스케이스 (사용자ID, 콘서트 날짜, 좌석 번호)
    @Transactional
    public SeatReservation reserveSeat(Long userId, LocalDate concertDate, int seatNumber) {

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다."));

        // 콘서트 일정 조회
        ConcertSchedule schedule = scheduleRepository.findByConcertDate(concertDate)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 날짜에 해당하는 콘서트가 존재하지 않습니다."));

        // 좌석 조회 (예약 가능한 상태의 좌석만 조회)
        Seat seat = seatRepository.findByConcertSchedule_ScheduleIdAndSeatNumberAndStatus(schedule.getScheduleId(), seatNumber, SeatStatus.FREE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 좌석은 존재하지 않거나 이미 예약된 좌석입니다."));

        // 좌석 상태 임시예약으로 변경
        seat.reserveTemporarily();

        // 임시 예약 객체 생성
        SeatReservation reservation = SeatReservation.createTemporary(user.getUserId(), seat.getSeatId());
        return reservationRepository.save(reservation);
    }

}
