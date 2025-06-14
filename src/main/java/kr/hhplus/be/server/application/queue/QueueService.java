package kr.hhplus.be.server.application.queue;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.ConcertScheduleRepository;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class QueueService {

    private static final int MAX_ACTIVE_TOKEN_SIZE = 50;    // 동시 입장 허용 인원
    private static final long QUEUE_EXPIRES_TIME = 5L;     // 토큰 만료 시간 30분

    private final QueueTokenRepository queueTokenRepository;
    private final UserRepository userRepository;
    private final ConcertScheduleRepository scheduleRepository;


    /**
     * # Method설명 : 대기열 토큰 발급 (없으면 신규, 있으면 기존 토큰 반환)
     * # MethodName : issueQueueToken
     **/
    @Transactional
    public QueueToken issueQueueToken(Long userId, Long scheduleId) {

        validateUserId(userId); // 사용자 검증
        validateScheduleId(scheduleId); // 콘서트 일정 검증

        // 기존 토큰 조회 -> 있으면 그대로 반환
        Optional<String> tokenIdOpt = queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, scheduleId);
        if (tokenIdOpt.isPresent()) {
            return queueTokenRepository.findQueueTokenByTokenId(tokenIdOpt.get())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "대기열 토큰이 존재하지 않습니다."));
        }

        // 현재 ACTIVE 토큰 수 확인
        int activeTokens = queueTokenRepository.countActiveTokens(scheduleId);

        // 새 토큰 생성 (정책에 따라 ACTIVE/WAITING)
        String tokenId = UUID.randomUUID().toString();

        QueueToken queueToken;
        if (activeTokens < MAX_ACTIVE_TOKEN_SIZE) { // 바로 ACTIVE
            queueToken = QueueToken.activeToken(tokenId, userId, scheduleId, QUEUE_EXPIRES_TIME);
        } else {
            int waitingTokens = queueTokenRepository.countWaitingTokens(scheduleId);    // WAITING - 순번 계산
            queueToken = QueueToken.waitingToken(tokenId, userId, scheduleId, waitingTokens);
        }
        queueTokenRepository.save(queueToken);
        return queueToken;
    }


    /**
     * # Method설명 : 대기열 토큰/상태/순번 조회
     * # MethodName : getQueueInfo
     **/
    @Transactional(readOnly = true)
    public QueueToken getQueueInfo(Long scheduleId, String tokenId) {
        validateScheduleId(scheduleId);

        QueueToken queueToken = queueTokenRepository.findQueueTokenByTokenId(tokenId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 대기열 토큰"));
        if (queueToken.isExpired())
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "대기열 토큰이 만료되었습니다.");

        if (queueToken.isActive())
            return queueToken;

        // 대기중인 경우 순번 설정
        int waitingPosition = queueTokenRepository.findWaitingPosition(queueToken).orElse(0);
        return queueToken.withWaitingPosition(waitingPosition);
    }

    /**
     * # Method설명 : 콘서트 일정별 대기열 토큰 ACTIVE 상태로 변경
     * # MethodName : promoteWaitingToActive
     **/
    @Transactional
    public void promoteWaitingToActive(Long scheduleId) {
        int activeCount = queueTokenRepository.countActiveTokens(scheduleId);

        // ACTIVE 자리가 남아 있는 동안 반복 변경
        while (activeCount < MAX_ACTIVE_TOKEN_SIZE) {
            // WAITING ZSet에서 0번 순번 가져오기
            Optional<String> firstWaitingTokenIdOpt = queueTokenRepository.findFirstWaitingTokenId(scheduleId);
            if (firstWaitingTokenIdOpt.isEmpty()) break; // 대기자 없으면 끝

            String tokenId = firstWaitingTokenIdOpt.get();
            QueueToken token = queueTokenRepository.findQueueTokenByTokenId(tokenId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "대기열 토큰이 존재하지 않습니다."));

            // ACTIVE로 변경
            QueueToken promoted = token.promoteToActive(QUEUE_EXPIRES_TIME);
            queueTokenRepository.save(promoted);

            activeCount++;
        }
    }

    /**
     * # Method설명 : 사용자 검증
     * # MethodName : validateUserId
     **/
    private void validateUserId(Long userId) {
        if (!userRepository.existsById(userId))
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }

    /**
     * # Method설명 : 콘서트 일정 검증
     * # MethodName : validateScheduleId
     **/
    private void validateScheduleId(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId))
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "콘서트 일정을 찾을 수 없습니다.");
    }


}
