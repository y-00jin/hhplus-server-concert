package kr.hhplus.be.server.user.dto;

import kr.hhplus.be.server.user.domain.enums.UserBalanceType;

public record UserBalanceRequest(
        Long userId,
        long amount,
        UserBalanceType type,
        String description
) {}