package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserBalanceServiceConcurrencyTest {


    @Autowired
    UserRepository userRepository;

    @Autowired
    UserBalanceRepository userBalanceRepository;

    @Autowired
    UserService userService;

    private Long userId;
    private LocalDateTime now;

    @BeforeEach
    void setup() {
        now = LocalDateTime.now();
        // 사용자 등록
        userId = userRepository.save(new User(null, UUID.randomUUID().toString(), "test@email.com", "pw", "사용자", now, now)).getUserId();

        // 포인트 충전
        userService.chargeBalance(userId, 10000L);
    }

    @AfterEach
    void cleanUp() {
        userBalanceRepository.deleteAllForTest();
        userRepository.deleteAllForTest();
    }


    @Test
    void 동시_잔액충전_테스트() throws Exception {
        // given
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    userService.chargeBalance(userId, 10000L);
                    results.add(true);
                } catch (Exception e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        long successCount = results.stream().filter(r -> r).count();
        long failCount = results.stream().filter(r -> !r).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(threadCount - successCount);

        // 최종 잔액 확인
        UserBalance balance = userService.getCurrentBalance(userId);
        System.out.println(balance);

        assertThat(successCount).isEqualTo(threadCount);
        assertThat(balance.getCurrentBalance()).isEqualTo(10000L * (threadCount + 1)); // 최초 1회 + threadCount번 충전
    }



    @Test
    void 동시_잔액사용_결제_테스트() throws Exception {
        // given
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    userService.useBalance(userId, 7000L);
                    results.add(true);
                } catch (Exception e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        // 한 번만 성공, 나머지는 실패
        long successCount = results.stream().filter(r -> r).count();
        long failCount = results.stream().filter(r -> !r).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(threadCount - successCount);

        // 최종 잔액은 확인
        UserBalance balance = userService.getCurrentBalance(userId);
        assertThat(balance.getCurrentBalance()).isEqualTo(3000L);
    }
}
