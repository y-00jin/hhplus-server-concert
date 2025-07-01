package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    void 동시_잔액사용_결제_테스트() throws Exception {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    UserBalance result = userService.useBalance(userId, 7000L);
                    results.add(true);
                    log.info("포인트 사용 성공 - 사용 후 잔액 = {}", result.getCurrentBalance());
                } catch (Exception e) {
                    results.add(false);
                    log.error("포인트 사용 실패 ");
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


    @Test
    void 동시_잔액충전_테스트() throws Exception {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    UserBalance result = userService.chargeBalance(userId, 10000L);
                    results.add(true);
                    log.info("포인트 충전 성공 - 사용 후 잔액 = {}", result.getCurrentBalance());
                } catch (Exception e) {
                    results.add(false);
                    log.error("포인트 충전 실패 ");
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        // 전체 성공
        long successCount = results.stream().filter(r -> r).count();
        assertThat(successCount).isEqualTo(threadCount);

        // 최종 잔액 확인
        UserBalance balance = userService.getCurrentBalance(userId);
        assertThat(balance.getCurrentBalance()).isEqualTo(10000L * (successCount+1));
    }
}
