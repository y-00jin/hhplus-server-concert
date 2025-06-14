package kr.hhplus.be.server.domain.user;

public enum UserBalanceType {
    CHARGE("충전"),
    USE("사용"),
    CANCEL("취소"),
    REFUND("환불");

    private final String description; // 한글 등 추가 필드

    UserBalanceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
