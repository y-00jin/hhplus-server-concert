package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.api.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    UserRepository userRepository = mock(UserRepository.class);
    UserBalanceRepository userBalanceRepository = mock(UserBalanceRepository.class);

    UserService userService;

    Long userId = 1L;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userBalanceRepository);
        user = new User(userId, null, "test@test.com", "1234", "테스트", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void 사용자_조회_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        User result = userService.getUser(userId);

        // then
        assertThat(result).isEqualTo(user);
    }

    @Test
    void 사용자_조회_실패_존재하지_않음() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    @Test
    void 잔액_조회_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserBalance latest = UserBalance.charge(userId, 50000L, 0);
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId))
                .thenReturn(Optional.of(latest));

        // when
        UserBalance result = userService.getCurrentBalance(userId);

        // then
        assertThat(result.getCurrentBalance()).isEqualTo(50000L);
    }

    @Test
    void 잔액_조회_없을_때_기본값_반환() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId))
                .thenReturn(Optional.empty());

        // when
        UserBalance result = userService.getCurrentBalance(userId);

        // then
        assertThat(result.getCurrentBalance()).isEqualTo(0L);
    }

    @Test
    void 잔액_충전_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        long amount = 30000L;
        long currentBalance = 20000L;
        UserBalance current = new UserBalance(
                null,
                userId,
                0L,  // 과거 거래의 금액 (테스트상 의미 없음)
                null,
                currentBalance,
                null,
                null
        );
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId)).thenReturn(Optional.of(current));
        when(userBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserBalance result = userService.chargeBalance(userId, amount);

        // then
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getCurrentBalance()).isEqualTo(amount + currentBalance);

        // given
    }

    @Test
    void 잔액_충전_실패_사용자_없음() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.chargeBalance(userId, 10000L))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    @Test
    void 사용자아이디로_잔액_충전_실패_최소금액미만() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // when & then
        assertThatThrownBy(() ->  userService.chargeBalance(userId, 0L))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void 잔액_사용_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        long amount = 10000L;
        long currentBalance = 30000L;

        UserBalance current = new UserBalance(
                null,
                userId,
                0L,  // 과거 거래의 금액 (테스트상 의미 없음)
                null,
                currentBalance,
                null,
                null
        );
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId)).thenReturn(Optional.of(current));
        when(userBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserBalance result = userService.useBalance(userId, amount);

        // then
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getCurrentBalance()).isEqualTo(currentBalance - amount);
    }

    @Test
    void 잔액_사용_실패_사용자_없음() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.useBalance(userId, 10000L))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    @Test
    void 사용자아이디로_잔액_사용_실패_최소금액미만() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() ->  userService.useBalance(userId, 0L))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void 사용자아이디로_포인트_사용_실패_잔액부족() {

        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        long currentBalance = 10000L;

        UserBalance current = new UserBalance(
                1L,
                userId,
                currentBalance,
                UserBalanceType.CHARGE,
                currentBalance,
                null,
                null
        );
        when(userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId)).thenReturn(Optional.of(current));

        // when & then
        assertThatThrownBy(() -> userService.useBalance(userId, 50000L))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
        verify(userRepository, times(1)).findById(userId);
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId);
    }
}