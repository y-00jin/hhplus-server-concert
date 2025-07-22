package kr.hhplus.be.server.user.dto.response;

import kr.hhplus.be.server.user.domain.userBalance.UserBalanceType;
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
