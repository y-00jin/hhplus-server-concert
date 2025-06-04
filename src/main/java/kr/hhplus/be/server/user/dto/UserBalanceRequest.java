package kr.hhplus.be.server.user.dto;

import kr.hhplus.be.server.user.domain.enums.UserBalanceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserBalanceRequest{

    private Long userId;
    private long amount;
    private UserBalanceType type;
    private String description;

}