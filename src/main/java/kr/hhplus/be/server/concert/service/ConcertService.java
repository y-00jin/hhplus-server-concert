package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.concert.dto.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.dto.SeatResponse;

import java.time.LocalDate;
import java.util.List;

public interface ConcertService {

    /**
     * # Method설명 : 예약 가능 콘서트 일정 조회 (오늘 이후)
     * # MethodName : getAvailableSchedules
     **/
    List<ConcertScheduleResponse> getAvailableSchedules();

    /**
     * # Method설명 : 특정 날짜로 예약 가능 좌석 목록 조회
     * # MethodName : getAvailableSeats
     **/
    List<SeatResponse> getAvailableSeatsByDate(LocalDate date);

}
