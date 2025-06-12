package kr.hhplus.be.server.domain.queue;

import java.time.LocalDateTime;

public class QueueToken {
    private String token;            // 대기열 토큰 (랜덤 문자열, UUID 등)
    private Long userId;             // 사용자 PK (user_id)
    private Long scheduleId;         // 콘서트 일정 PK
    private Long seatId;             // 좌석 PK
    private Integer queuePosition;   // 대기열 순서
    private QueueStatus status;      // 상태
    private LocalDateTime issuedAt;  // 토큰 발급 시각
    private LocalDateTime expiresAt; // 토큰 만료 시각

    public QueueToken(String token, Long userId, Long scheduleId, Long seatId, Integer queuePosition, QueueStatus status, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
        this.queuePosition = queuePosition;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    /**
     * # Method설명 : 활성(대기열 통과) 토큰 생성
     * # MethodName : activeToken
     **/
    public static QueueToken activeToken(String token, Long userId, Long scheduleId, Long seatId, long expiresInMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new QueueToken(
                token,
                userId,
                scheduleId,
                seatId,
                null,
                QueueStatus.ACTIVE,
                now,
                now.plusMinutes(expiresInMinutes)
        );
    }

    /**
     * # Method설명 : 대기열(기다림) 상태 토큰 생성
     * # MethodName : waitingToken
     **/
    public static QueueToken waitingToken(String token, Long userId, Long scheduleId, Long seatId, int waitingPosition) {
        LocalDateTime now = LocalDateTime.now();
        return new QueueToken(
                token,
                userId,
                scheduleId,
                seatId,
                waitingPosition,
                QueueStatus.WAITING,
                now,
                null
        );
    }

    /**
     * # Method설명 : 토큰이 활성 상태인지 확인
     * # MethodName : isActive
     **/
    public boolean isActive() {
        return status == QueueStatus.ACTIVE;
    }

    /**
     * # Method설명 : 토큰이 만료됐는지 확인
     * # MethodName : isExpired
     **/
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }


    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public Long getScheduleId() { return scheduleId; }
    public Long getSeatId() { return seatId; }
    public Integer getQueuePosition() { return queuePosition; }
    public QueueStatus getStatus() { return status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }


}
