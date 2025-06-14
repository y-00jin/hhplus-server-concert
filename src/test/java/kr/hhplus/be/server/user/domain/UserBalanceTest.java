package kr.hhplus.be.server.user.domain;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.user.domain.enums.UserBalanceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import java.nio.ByteBuffer;

class UserBalanceTest {

    private User makeUser() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return User.builder()
                .uuid(buffer.array())
                .email("test@a.com")
                .password("pw")
                .userNm("테스터")
                .build();
    }

    @Test
    void charge_정상() {
        User user = makeUser();
        UserBalance balance = UserBalance.charge(user, 5000L, 1000L);
        assertThat(balance.getCurrentBalance()).isEqualTo(6000L);
        assertThat(balance.getType()).isEqualTo(UserBalanceType.CHARGE);
    }

    @Test
    void charge_0원이하_예외() {
        User user = makeUser();
        assertThatThrownBy(() -> UserBalance.charge(user, 0L, 1000L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("충전 금액은 1원 이상");
    }

    @Test
    void use_정상() {
        User user = makeUser();
        UserBalance balance = UserBalance.use(user, 2000L, 3000L);
        assertThat(balance.getCurrentBalance()).isEqualTo(1000L);
        assertThat(balance.getType()).isEqualTo(UserBalanceType.USE);
    }

    @Test
    void use_잔액부족_예외() {
        User user = makeUser();
        assertThatThrownBy(() -> UserBalance.use(user, 5000L, 2000L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    void use_0원이하_예외() {
        User user = makeUser();
        assertThatThrownBy(() -> UserBalance.use(user, 0L, 2000L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("사용 금액은 1원 이상");
    }
}