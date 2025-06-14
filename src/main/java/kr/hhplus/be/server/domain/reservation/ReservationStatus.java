package kr.hhplus.be.server.domain.reservation;

public enum ReservationStatus {

    TEMP_RESERVED("임시 예약"),
    EXPIRED("만료"),
    CANCELLED("취소"),
    CONFIRMED("예약 확정");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
