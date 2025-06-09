package kr.hhplus.be.server.user.repository;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserBalanceRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Test
    @DisplayName("userId로 가장 최신 잔액 내역 1건 조회")
    void findTopByUser_UserIdOrderByBalanceHistoryIdDesc() throws InterruptedException {

        // given: 유저 생성 및 저장
        // UUID 16바이트 변환
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        byte[] uuidBytes = buffer.array();

        User user = User.builder()
                .uuid(uuidBytes)
                .email("testuser@example.com")
                .password("1234")
                .userNm("테스터")
                .build();
        user = userRepository.save(user);

        // b1 저장 후 약간의 sleep
        UserBalance b1 = UserBalance.charge(user, 5000L, 0);
        userBalanceRepository.save(b1);

        Thread.sleep(1000);

        UserBalance b2 = UserBalance.charge(user, 2000L, b1.getCurrentBalance());
        userBalanceRepository.save(b2);

        // when: 최근 내역 1건 조회
        Optional<UserBalance> result = userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(user.getUserId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getCurrentBalance()).isEqualTo(7000L);
    }
}