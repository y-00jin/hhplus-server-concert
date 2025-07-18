package kr.hhplus.be.server.user.domain.userBalance;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;

import java.time.LocalDateTime;

public class UserBalance {

    private Long balanceHistoryId;
    private Long userId;
    private long amount;
    private UserBalanceType type;
    private long currentBalance;
    private String description;
    private LocalDateTime createdAt;

    public UserBalance(Long balanceHistoryId, Long userId, long amount, UserBalanceType type, long currentBalance, String description, LocalDateTime createdAt) {
        this.balanceHistoryId = balanceHistoryId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.currentBalance = currentBalance;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getBalanceHistoryId() {
        return balanceHistoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }

    public UserBalanceType getType() {
        return type;
    }

    public long getCurrentBalance() {
        return currentBalance;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void assignId(Long balanceHistoryId) {
        this.balanceHistoryId = balanceHistoryId;
    }

    public static UserBalance create(Long userId, long amount, UserBalanceType type, long currentBalance, String description) {
        return new UserBalance(null, userId, amount, type, currentBalance, description, LocalDateTime.now());
    }


    /**
     * # Method설명 : 잔액 충전 - UserBalance 검증 및 생성
     * # MethodName : charge
     **/
    public static UserBalance charge(Long userId, long amount, long currentBalance) {
        // 0원 이하 충전 불가능
        if (amount <= 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "충전 금액은 1원 이상이어야 합니다");
        }

        return create(userId, amount, UserBalanceType.CHARGE, currentBalance + amount, amount +"원 " + UserBalanceType.CHARGE.getDescription());
    }

    /**
     * # Method설명 : 잔액 사용 - UserBalance 검증 및 생성
     * # MethodName : use
     **/
    public static UserBalance use(Long userId, long amount, long currentBalance) {
        // 0원 이하 사용 불가능
        if(amount <= 0)
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "사용 금액은 1원 이상이어야 합니다");

        // 현재 잔고 < 사용 금액
        if(currentBalance < amount){
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "잔액이 부족합니다. (필요: " + amount + ", 보유: " + currentBalance + ")");
        }

        return create(userId, amount, UserBalanceType.USE, currentBalance - amount, amount +"원 " + UserBalanceType.USE.getDescription());
    }
}
