package kr.hhplus.be.server.domain.concert;

import java.util.List;

public interface ConcertSoldoutRankingRepository {
    List<String> getSoldoutRanking(String yearMonth, int topN);
    void addSoldoutRanking(String yearMonth, Long scheduleId, double elapsedSeconds);
    boolean isAlreadyRanked(String yearMonth, Long scheduleId);
    void evictSoldoutRanking(String yearMonth);
}
