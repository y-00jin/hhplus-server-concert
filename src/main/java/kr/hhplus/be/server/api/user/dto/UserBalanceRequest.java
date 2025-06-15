package kr.hhplus.be.server.api.user.dto;

import kr.hhplus.be.server.domain.user.UserBalanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBalanceRequest{
    private long amount;
}