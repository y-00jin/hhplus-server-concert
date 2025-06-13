package kr.hhplus.be.server.infrastructure.persistence.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.hhplus.be.server.domain.queue.QueueStatus;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisQueueTokenRepository implements QueueTokenRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 생성

    // 토큰 정보 전체를 저장할 때 쓰는 키(이름)
    private String tokenInfoKey(String token) {
        return "queue:token:" + token;
    }
    // userId와 scheduleId 조합으로 토큰ID를 저장/찾을 때 쓰는 키(이름)
    private String userScheduleKey(Long userId, Long scheduleId) {
        return "queue:user-schedule:" + userId + ":" + scheduleId;
    }
    // 콘서트별 ACTIVE(입장가능) ZSet의 key
    private String activeSetKey(Long scheduleId) {
        return "queue:active:" + scheduleId;
    }
    // 콘서트별 WAITING(대기중) ZSet의 key
    private String waitingSetKey(Long scheduleId) {
        return "queue:waiting:" + scheduleId;
    }

    /**
     * # Method설명 : 대기열 토큰 저장(생성/갱신)
     * # MethodName : save
     **/
    @Override
    public void save(QueueToken queueToken) {
        redisTemplate.opsForValue().set(tokenInfoKey(queueToken.getToken()), queueToken);   // 토큰 정보 전체 저장
        redisTemplate.opsForValue().set(userScheduleKey(queueToken.getUserId(), queueToken.getScheduleId()), queueToken.getToken());    // userId, scheduleId 조합으로 token(문자열) 저장

        if (queueToken.getStatus() == QueueStatus.ACTIVE) { // 활성화 상태
            saveActiveToken(queueToken);    // 입장 가능한 상태면 ACTIVE ZSet에
        } else {
            saveWaitingToken(queueToken);   // 대기열이면 WAITING ZSet에
        }
    }

    /**
     * # Method설명 : 특정 사용자/콘서트 일정 기준으로 토큰 id 조회
     * # MethodName : findTokenIdByUserIdAndScheduleId
     **/
    @Override
    public Optional<String> findTokenIdByUserIdAndScheduleId(Long userId, Long scheduleId) {
        Object token = redisTemplate.opsForValue().get(userScheduleKey(userId, scheduleId));    // userScheduleKey로 Redis에서 tokenId 조회
        return token != null ? Optional.of(token.toString()) : Optional.empty();    // 값이 있으면 Optional로 감싸서 반환, 없으면 Optional.empty()
    }

    /**
     * # Method설명 : 토큰 id로 토큰 전체 정보 조회
     * # MethodName : findQueueTokenByTokenId
     **/
    @Override
    public Optional<QueueToken> findQueueTokenByTokenId(String tokenId) {
        Object tokenObj = redisTemplate.opsForValue().get(tokenInfoKey(tokenId));
        if (tokenObj == null) return Optional.empty();

        if (tokenObj instanceof QueueToken token) {
            return Optional.of(token);
        }

        if (tokenObj instanceof java.util.Map map) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            QueueToken token = mapper.convertValue(map, QueueToken.class);
            return Optional.of(token);
        }

        return Optional.empty();
    }

    /**
     * # Method설명 : WAITING 중 ZSet에서 내 순번 조회
     * # MethodName : findWaitingPosition
     **/
    @Override
    public Optional<Integer> findWaitingPosition(QueueToken queueToken) {
        String waitingKey = waitingSetKey(queueToken.getScheduleId());  // 대기열 ZSet의 키
        String userSchedule = userScheduleKey(queueToken.getUserId(), queueToken.getScheduleId());   // 대기열 ZSet에 저장된 값: userId와 scheduleId를 묶은 문자열(key)
        Long rank = redisTemplate.opsForZSet().rank(waitingKey, userSchedule);  // ZSet에서 내 "랭크"를 가져옴 (0번부터 시작)
        return rank != null ? Optional.of(rank.intValue() + 1) : Optional.empty(); // 1-based 순번
    }

    /**
     * # Method설명 : 콘서트 일정별 현재 대기(WAITING) 토큰 개수 조회
     * # MethodName : countWaitingTokens
     **/
    @Override
    public int countWaitingTokens(Long scheduleId) {
        Long count = redisTemplate.opsForZSet().count( waitingSetKey(scheduleId), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        return count != null ? count.intValue() : 0;
    }

    /**
     * # Method설명 : 콘서트 일정별 현재 입장가능(ACTIVE) 토큰 개수 조회
     * # MethodName : countActiveTokens
     **/
    @Override
    public int countActiveTokens(Long scheduleId) {
        Long count = redisTemplate.opsForZSet().count(activeSetKey(scheduleId), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        return count != null ? count.intValue() : 0;
    }

    /**
     * # Method설명 : 토큰 만료 처리 (상태 변경)
     * # MethodName : expiresQueueToken
     **/
    @Override
    public void expiresQueueToken(String tokenId) {
        Optional<QueueToken> optToken = findQueueTokenByTokenId(tokenId);   // 토큰 전체 객체 조회
        if (optToken.isEmpty()) return;

        QueueToken queueToken = optToken.get();
        String infoKey = tokenInfoKey(queueToken.getToken());
        String userSchedule = userScheduleKey(queueToken.getUserId(), queueToken.getScheduleId());


        redisTemplate.delete(infoKey);  // 토큰 삭제
        redisTemplate.delete(userSchedule); // userId, scheduleId 매핑 정보 삭제

        if (queueToken.getStatus() == QueueStatus.ACTIVE) {
            redisTemplate.opsForZSet().remove(activeSetKey(queueToken.getScheduleId()), userSchedule);  // ACTIVE ZSet에서 삭제
        } else {
            redisTemplate.opsForZSet().remove(waitingSetKey(queueToken.getScheduleId()), userSchedule); // WAITING ZSet에서 삭제
        }
    }

    /**
     * # Method설명 : 대기자 중 첫번째 토큰 조회
     * # MethodName : findFirstWaitingTokenId
     **/
    @Override
    public Optional<String> findFirstWaitingTokenId(Long scheduleId) {
        String waitingKey = waitingSetKey(scheduleId);
        Set<Object> first = redisTemplate.opsForZSet().range(waitingKey, 0, 0);
        if (first == null || first.isEmpty()) return Optional.empty();

        String userSchedule = (String) first.iterator().next();
        // userSchedule → tokenId 변환
        Object token = redisTemplate.opsForValue().get(userSchedule);
        return token != null ? Optional.of(token.toString()) : Optional.empty();
    }




    // WAITING(대기 중) 상태 토큰 저장용
    private void saveWaitingToken(QueueToken queueToken) {
        String waitingKey = waitingSetKey(queueToken.getScheduleId());
        String userSchedule = userScheduleKey(queueToken.getUserId(), queueToken.getScheduleId());

        // issuedAt(토큰 발급시각)을 UTC 기준 초(epoch)로 바꿔 점수로 저장
        Instant issuedInstant = queueToken.getIssuedAt().atZone(ZoneOffset.UTC).toInstant();
        double score = issuedInstant.getEpochSecond();

        // ZSet(대기열 집합)에 추가: 순서가 오래된 순(먼저 들어온 순)이 우선
        redisTemplate.opsForZSet().add(waitingKey, userSchedule, score);

        // 토큰 정보/매핑 정보의 TTL: 24시간
        redisTemplate.expire(tokenInfoKey(queueToken.getToken()), Duration.ofHours(24));
        redisTemplate.expire(userSchedule, Duration.ofHours(24));
    }

    // ACTIVE(입장가능) 상태 토큰 저장용
    private void saveActiveToken(QueueToken queueToken) {
        String activeKey = activeSetKey(queueToken.getScheduleId());
        String userSchedule = userScheduleKey(queueToken.getUserId(), queueToken.getScheduleId());
        LocalDateTime expiresAt = queueToken.getExpiresAt();
        if (expiresAt == null) return; // null일 수 없음

        // expiresAt(토큰 만료시각)을 UTC 초(epoch)로 변환
        Instant expiresInstant = expiresAt.atZone(ZoneOffset.UTC).toInstant();
        double score = expiresInstant.getEpochSecond();

        // ZSet(ACTIVE 집합)에 추가: 만료시간이 가까운 순으로 자동 정렬됨
        redisTemplate.opsForZSet().add(activeKey, userSchedule, score);

        // 토큰 정보/매핑 정보 만료도 만료시각에 맞춤
        redisTemplate.expireAt(tokenInfoKey(queueToken.getToken()), expiresInstant);
        redisTemplate.expireAt(userSchedule, expiresInstant);
    }
}
