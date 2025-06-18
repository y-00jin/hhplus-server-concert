package kr.hhplus.be.server.api.concert;


import kr.hhplus.be.server.api.concert.dto.ConcertScheduleResponse;
import kr.hhplus.be.server.api.concert.dto.SeatResponse;
import kr.hhplus.be.server.application.concert.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/concerts")
public class ConcertController {

    private final ConcertService concertService;

    /**
     * # Method설명 : 예약 가능 콘서트 일정 목록 조회 (오늘 이후)
     * # MethodName : getAvailableSchedules
     **/
    @GetMapping("/schedules")
    public ResponseEntity<List<ConcertScheduleResponse>> getAvailableSchedules() {

        List<ConcertScheduleResponse> response = concertService.getAvailableSchedules()
                .stream()
                .map(data ->
                        ConcertScheduleResponse.builder()
                                .scheduleId(data.getScheduleId())
                                .concertDate(data.getConcertDate())
                                .build()
                        )
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * # Method설명 : 특정 날짜로 예약 가능 좌석 목록 조회
     * # MethodName : getAvailableSeatsByDate
     **/
    @GetMapping("/schedules/{date}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeatsByDate(@PathVariable("date") String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<SeatResponse> response = concertService.getAvailableSeatsByDate(localDate)
                .stream()
                .map(data ->
                        SeatResponse.builder()
                                .seatId(data.getSeatId())
                                .scheduleId(data.getScheduleId())
                                .seatNumber(data.getSeatNumber())
                                .price(data.getPrice())
                                .status(data.getStatus())
                                .build()
                        )
                .toList();
        return ResponseEntity.ok(response);
    }
}
