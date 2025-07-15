package kr.hhplus.be.server.domain.user;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.user.domain.userBalance.UserBalance;
import kr.hhplus.be.server.user.domain.userBalance.UserBalanceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.*;

class UserBalanceTest {

    @Test
    void create_정상_생성() {
        // given
        Long userId = 1L;
        long amount = 5000L;
        UserBalanceType type = UserBalanceType.CHARGE;
        long currentBalance = 15000L;
        String description = "테스트";

        // when
        UserBalance balance = UserBalance.create(userId, amount, type, currentBalance, description);

        // then
        assertThat(balance.getUserId()).isEqualTo(userId);
        assertThat(balance.getAmount()).isEqualTo(amount);
        assertThat(balance.getType()).isEqualTo(type);
        assertThat(balance.getCurrentBalance()).isEqualTo(currentBalance);
        assertThat(balance.getDescription()).isEqualTo(description);
        assertThat(balance.getCreatedAt()).isNotNull();
    }

    @Test
    void assignId_정상_동작() {
        // given
        UserBalance balance = UserBalance.create(2L, 10000L, UserBalanceType.CHARGE, 20000L, "설명");

        // when
        balance.assignId(10L);

        // then
        assertThat(balance.getBalanceHistoryId()).isEqualTo(10L);
    }

    @Test
    void charge_정상_생성() {
        // given
        Long userId = 1L;
        long amount = 5000L;
        long currentBalance = 20000L;

        // when
        UserBalance charged = UserBalance.charge(userId, amount, currentBalance);

        // then
        assertThat(charged.getType()).isEqualTo(UserBalanceType.CHARGE);
        assertThat(charged.getCurrentBalance()).isEqualTo(currentBalance + amount);
        assertThat(charged.getAmount()).isEqualTo(amount);
        assertThat(charged.getDescription()).contains("충전");
    }

    @Test
    void charge_실패_0원이하() {
        // given
        Long userId = 1L;
        long amount = 0L;
        long currentBalance = 10000L;

        // when & then
        assertThatThrownBy(() -> UserBalance.charge(userId, amount, currentBalance))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void use_정상_사용() {
        // given
        Long userId = 1L;
        long amount = 4000L;
        long currentBalance = 8000L;

        // when
        UserBalance used = UserBalance.use(userId, amount, currentBalance);

        // then
        assertThat(used.getType()).isEqualTo(UserBalanceType.USE);
        assertThat(used.getCurrentBalance()).isEqualTo(currentBalance - amount);
        assertThat(used.getAmount()).isEqualTo(amount);
        assertThat(used.getDescription()).contains("사용");
    }

    @Test
    void use_실패_0원이하() {
        // given
        Long userId = 1L;
        long amount = 0L;
        long currentBalance = 10000L;

        // when & then
        assertThatThrownBy(() -> UserBalance.use(userId, amount, currentBalance))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void use_실패_잔액부족() {
        // given
        Long userId = 1L;
        long amount = 15000L;
        long currentBalance = 10000L;

        // when & then
        assertThatThrownBy(() -> UserBalance.use(userId, amount, currentBalance))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }
}
