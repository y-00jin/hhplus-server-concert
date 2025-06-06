package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.dto.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.dto.SeatResponse;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Service
public class ConcertServiceImpl implements ConcertService {

    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;


    /**
     * # Method설명 : 예약 가능 콘서트 일정 조회 (오늘 이후)
     * # MethodName : getAvailableSchedules
     **/
    @Override
    public List<ConcertScheduleResponse> getAvailableSchedules() {
        return scheduleRepository
                .findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate.now())
                .stream()
                .map(ConcertSchedule::toResponse)
                .toList();
    }

    /**
     * # Method설명 : 특정 콘서트일정ID로 예약 가능 좌석 목록 조회
     * # MethodName : getAvailableSeats
     **/
    @Override
    public List<SeatResponse> getAvailableSeatsByDate(LocalDate date) {

        // 콘서트 일정 조회 (날짜로)
        ConcertSchedule schedule = scheduleRepository.findByConcertDate(date)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 날짜의 콘서트 일정이 없습니다."));

        // 좌석 조회 (해당 일정ID 기준)
        return seatRepository
                .findAllByConcertSchedule_ScheduleIdAndStatus(schedule.getScheduleId(), SeatStatus.FREE)
                .stream()
                .map(Seat::toResponse)
                .toList();

    }
}
