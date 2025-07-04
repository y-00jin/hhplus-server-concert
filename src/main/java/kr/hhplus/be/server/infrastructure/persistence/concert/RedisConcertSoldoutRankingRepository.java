package kr.hhplus.be.server.infrastructure.persistence.concert;

import kr.hhplus.be.server.domain.concert.ConcertSoldoutRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisConcertSoldoutRankingRepository implements ConcertSoldoutRankingRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RANKING_KEY_PREFIX = "ranking:soldout-time:";

    // 월별 매진 랭킹 조회
    @Override
    public List<String> getSoldoutRanking(String yearMonth, int topN) {
        String key = RANKING_KEY_PREFIX + yearMonth;
        // 가장 빠른 매진 순으로 TOP N 조회 (score 오름차순)
        Set<String> ranking = redisTemplate.opsForZSet().range(key, 0, topN - 1);
        return ranking == null ? List.of() : List.copyOf(ranking);
    }

    // 랭킹 등록 (매진 완료 시, score: 경과 시간)
    @Override
    public void addSoldoutRanking(String yearMonth, Long scheduleId, double elapsedSeconds) {
        String key = RANKING_KEY_PREFIX + yearMonth;
        String member = "schedule:" + scheduleId;
        redisTemplate.opsForZSet().add(key, member, elapsedSeconds);
    }

    // 이미 랭킹에 등록된 schedule인지 확인 (중복 등록 방지)
    @Override
    public boolean isAlreadyRanked(String yearMonth, Long scheduleId) {
        String key = RANKING_KEY_PREFIX + yearMonth;
        String member = "schedule:" + scheduleId;
        Double score = redisTemplate.opsForZSet().score(key, member);
        return score != null;
    }

    // 월별 랭킹 전체 삭제
    @Override
    public void evictSoldoutRanking(String yearMonth) {
        String key = RANKING_KEY_PREFIX + yearMonth;
        redisTemplate.delete(key);
    }
}
