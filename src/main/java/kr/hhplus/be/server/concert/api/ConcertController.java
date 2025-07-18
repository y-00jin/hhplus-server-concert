package kr.hhplus.be.server.concert.api;


import kr.hhplus.be.server.concert.application.ConcertService;
import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertSchedule;
import kr.hhplus.be.server.concert.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.dto.response.ConcertSoldoutRankingResponse;
import kr.hhplus.be.server.concert.dto.response.SeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

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

    @GetMapping("/rankings/soldout")
    public ResponseEntity<List<ConcertSoldoutRankingResponse>> getSoldoutRanking(@RequestParam(name = "year",required = false) Integer year, @RequestParam(name = "month",required = false) Integer month, @RequestParam(name = "topN",required = false) Integer topN) {
        List<ConcertSchedule> resultList = concertService.getSoldoutRanking(year, month, topN);

        List<ConcertSoldoutRankingResponse> response =
                IntStream.range(0, resultList.size())
                        .mapToObj(i -> ConcertSoldoutRankingResponse.builder()
                                .scheduleId(resultList.get(i).getScheduleId())
                                .concertDate(resultList.get(i).getConcertDate())
                                .ranking(i + 1)
                                .build())
                        .toList();

        return ResponseEntity.ok(response);
    }
}
