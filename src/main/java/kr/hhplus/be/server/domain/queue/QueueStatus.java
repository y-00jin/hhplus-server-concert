package kr.hhplus.be.server.domain.queue;

public enum QueueStatus {
    WAITING("대기"),
    ACTIVE("활성");

    private final String description;

    QueueStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
