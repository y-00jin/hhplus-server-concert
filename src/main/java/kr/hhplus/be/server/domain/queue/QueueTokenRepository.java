package kr.hhplus.be.server.domain.queue;

import java.util.Optional;

public interface QueueTokenRepository {

    /**
     * # Method설명 : 대기열 토큰 저장(생성/갱신)
     * # MethodName : save
     **/
    void save(QueueToken queueToken);

    /**
     * # Method설명 : 특정 사용자/콘서트 일정 기준으로 토큰 id 조회
     * # MethodName : findTokenIdByUserIdAndScheduleId
     **/
    Optional<String> findTokenIdByUserIdAndScheduleId(Long userId, Long scheduleId);

    /**
     * # Method설명 : 토큰 id로 토큰 전체 정보 조회
     * # MethodName : findQueueTokenByTokenId
     **/
    Optional<QueueToken> findQueueTokenByTokenId(String tokenId);

    /**
     * # Method설명 : 주어진 토큰의 대기 순서(포지션) 조회
     * # MethodName : findWaitingPosition
     **/
    Optional<Integer> findWaitingPosition(QueueToken queueToken);


    /**
     * # Method설명 : 콘서트 일정별 현재 대기(WAITING) 토큰 개수 조회
     * # MethodName : countWaitingTokens
     **/
    int  countWaitingTokens(Long scheduleId);

    /**
     * # Method설명 : 콘서트 일정별 현재 활성(ACTIVE) 토큰 개수 조회
     * # MethodName : countActiveTokens
     **/
    int  countActiveTokens(Long scheduleId);


    /**
     * # Method설명 : 토큰 만료 처리 (상태 변경)
     * # MethodName : expiresQueueToken
     **/
    void expiresQueueToken(String tokenId);

    /**
     * # Method설명 : 대기자 중 첫번째 토큰 조회
     * # MethodName : findFirstWaitingTokenId
     **/
    Optional<String> findFirstWaitingTokenId(Long scheduleId);
}
