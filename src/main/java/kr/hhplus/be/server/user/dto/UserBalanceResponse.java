package kr.hhplus.be.server.user.dto;

import kr.hhplus.be.server.user.domain.enums.UserBalanceType;

public record UserBalanceResponse(
        Long balanceHistoryId,
        Long userId,
        long amount,
        UserBalanceType type,
        long currentBalance,
        String description

) {
}
