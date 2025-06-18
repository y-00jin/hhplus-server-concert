package kr.hhplus.be.server.api.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueTokenRequest {
    private Long userId;
    private Long scheduleId;
}
