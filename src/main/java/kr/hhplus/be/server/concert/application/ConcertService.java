package kr.hhplus.be.server.concert.application;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.concertSchedule.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.seat.Seat;
import kr.hhplus.be.server.concert.domain.seat.SeatRepository;
import kr.hhplus.be.server.concert.domain.seat.SeatStatus;
import kr.hhplus.be.server.concert.domain.soldoutRanking.ConcertSoldoutRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ConcertService {

    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final ConcertSoldoutRankingRepository concertSoldoutRankingRepository;

    @Value("${app.concert.ranking.soldout.default-top-n}")
    private int defaultTopN;

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

    public List<ConcertSchedule> getSoldoutRanking(Integer year, Integer month, Integer topN) {
        // 1. 연/월 없는 경우 이번 달
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();
        int n = (topN != null) ? topN : defaultTopN;
        String yearMonth = String.format("%04d%02d", y, m);

        List<Long> scheduleIds = concertSoldoutRankingRepository.getSoldoutRanking(yearMonth, n)
                .stream()
                .map(member -> Long.parseLong(member.replace("schedule:", "")))
                .toList();

        if (scheduleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 콘서트 일정 정보 조회
        List<ConcertSchedule> schedules = scheduleRepository.findAllByScheduleIdIn(scheduleIds);

        // ID → Entity 맵핑
        Map<Long, ConcertSchedule> map = schedules.stream()
                .collect(Collectors.toMap(ConcertSchedule::getScheduleId, s -> s));

        // 랭킹 순서대로 정렬
        return scheduleIds.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public void checkAndRegisterSoldoutRanking(Long scheduleId) {

        // 전체 좌석 수
        int totalSeats = seatRepository.countByConcertSchedule_ScheduleId(scheduleId);
        // CONFIRMED 좌석 수
        int confirmedSeats = seatRepository.countByConcertSchedule_ScheduleIdAndStatus(scheduleId, SeatStatus.CONFIRMED);

        // 이미 랭킹 등록됐는지 Redis에서 확인 (중복 등록 방지)
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = String.format("%04d%02d", now.getYear(), now.getMonthValue());
        boolean alreadyRanked = concertSoldoutRankingRepository.isAlreadyRanked(yearMonth, scheduleId);
        if (alreadyRanked) return;

        if (totalSeats == confirmedSeats) { // 매진 상태
            // 매진까지 걸린 시간 계산
            LocalDateTime createdAt = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "일정 정보 없음"))
                    .getCreatedAt();
            long elapsedSeconds = java.time.Duration.between(createdAt, now).getSeconds();

            // Redis에 랭킹 등록
            concertSoldoutRankingRepository.addSoldoutRanking(yearMonth, scheduleId, elapsedSeconds);
        }
    }

}
