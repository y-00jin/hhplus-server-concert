package kr.hhplus.be.server.domain.payment;

public enum PaymentStatus {

    SUCCESS("결제 성공"),
    FAILURE("결제 실패");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
