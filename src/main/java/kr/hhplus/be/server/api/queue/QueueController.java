package kr.hhplus.be.server.api.queue;

import kr.hhplus.be.server.api.queue.dto.QueueTokenRequest;
import kr.hhplus.be.server.api.queue.dto.QueueTokenResponse;
import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.domain.queue.QueueToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    /**
     * # Method설명 : 대기열 토큰 발급 요청
     * # MethodName : issueQueueToken
     **/
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> issueQueueToken(@RequestBody QueueTokenRequest request) {
        QueueToken queueToken = queueService.issueQueueToken(request.getUserId(), request.getScheduleId());
        return ResponseEntity.ok(toResponse(queueToken));
    }

    /**
     * # Method설명 : 대기열 토큰 조회
     * # MethodName : getQueueTokenInfo
     **/
    @GetMapping("/token")
    public ResponseEntity<QueueTokenResponse> getQueueTokenInfo(@RequestParam Long scheduleId,@RequestParam String tokenId) {
        QueueToken queueToken = queueService.getQueueInfo(scheduleId, tokenId);
        return ResponseEntity.ok(toResponse(queueToken));
    }

    private QueueTokenResponse toResponse(QueueToken queueToken){
        return QueueTokenResponse.builder()
                .token(queueToken.getToken())
                .userId(queueToken.getUserId())
                .scheduleId(queueToken.getScheduleId())
                .queuePosition(queueToken.getQueuePosition())
                .status(queueToken.getStatus().name())
                .issuedAt(queueToken.getIssuedAt() != null ? queueToken.getIssuedAt().toString() : null)
                .expiresAt(queueToken.getExpiresAt() != null ? queueToken.getExpiresAt().toString() : null)
                .build();
    }
}
