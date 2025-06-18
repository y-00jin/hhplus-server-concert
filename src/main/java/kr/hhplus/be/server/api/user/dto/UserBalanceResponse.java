package kr.hhplus.be.server.api.user.dto;

import kr.hhplus.be.server.domain.user.UserBalanceType;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class UserBalanceResponse {

    private Long balanceHistoryId;
    private Long userId;
    private long amount;
    private UserBalanceType type;
    private long currentBalance;
    private String description;

}
