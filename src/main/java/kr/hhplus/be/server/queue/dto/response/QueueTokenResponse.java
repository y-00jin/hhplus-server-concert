package kr.hhplus.be.server.queue.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueTokenResponse {
    private String token;
    private Long userId;
    private Long scheduleId;
    private Integer queuePosition;
    private String status;
    private String issuedAt;
    private String expiresAt;
}
