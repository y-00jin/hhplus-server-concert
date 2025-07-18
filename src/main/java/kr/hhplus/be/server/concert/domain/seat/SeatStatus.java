package kr.hhplus.be.server.concert.domain.seat;

public enum SeatStatus {

    FREE("예약 가능"),
    TEMP_RESERVED("임시 예약"),
    CONFIRMED("예약 확정");

    private final String description; // 한글 등 추가 필드

    SeatStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
