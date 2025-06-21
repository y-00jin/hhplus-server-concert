package kr.hhplus.be.server.application.concert;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ConcertService {

    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    /**
     * # Method설명 : 예약 가능 콘서트 일정 조회 (오늘 이후)
     * # MethodName : getAvailableSchedules
     **/
    public List<ConcertSchedule> getAvailableSchedules() {
        return scheduleRepository
                .findAllByConcertDateGreaterThanEqualOrderByConcertDateAsc(LocalDate.now());
    }

    /**
     * # Method설명 : 특정 콘서트일정ID로 예약 가능 좌석 목록 조회
     * # MethodName : getAvailableSeatsByDate
     **/
    public List<Seat> getAvailableSeatsByDate(LocalDate date) {

        // 콘서트 일정 조회 (날짜로)
        ConcertSchedule schedule = scheduleRepository.findByConcertDate(date)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 날짜("+date+")의 콘서트 일정이 없습니다."));

        // 좌석 조회 (해당 일정ID 기준)
        return seatRepository.findAllByConcertSchedule_ScheduleIdAndStatus(schedule.getScheduleId(), SeatStatus.FREE);
    }

}
