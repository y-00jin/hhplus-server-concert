package kr.hhplus.be.server.api.queue.dto;

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
