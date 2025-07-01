package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;

    @Mock
    UserBalanceRepository userBalanceRepository;
    @Mock
    DistributedLockRepository distributedLockRepository;

    Long userId = 1L;
    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class); // 필요시만 생성
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

        when(distributedLockRepository.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);
        when(userBalanceRepository.findCurrentBalanceByUserId(userId)).thenReturn(currentBalance);
        when(userBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));


        // when
        UserBalance result = userService.chargeBalance(userId, amount);

        // then
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getCurrentBalance()).isEqualTo(amount + currentBalance);

        // 락 해제까지 정상 호출되었는지
        verify(distributedLockRepository, times(1)).unlock(anyString(), anyString());
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));
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
        when(distributedLockRepository.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);

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
        when(distributedLockRepository.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);

        long amount = 10000L;
        long currentBalance = 30000L;

        // 분산락 획득 후 DB에서 현재 잔액 조회
        when(userBalanceRepository.findCurrentBalanceByUserId(userId)).thenReturn(currentBalance);

        // save 호출 시 저장 객체 반환
        when(userBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserBalance result = userService.useBalance(userId, amount);

        // then
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getCurrentBalance()).isEqualTo(currentBalance - amount);

        verify(distributedLockRepository, times(1)).tryLock(anyString(), anyString(), anyLong());
        verify(distributedLockRepository, times(1)).unlock(anyString(), anyString());
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
        when(distributedLockRepository.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);

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
        when(distributedLockRepository.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);

        long currentBalance = 10000L;
        when(userBalanceRepository.findCurrentBalanceByUserId(userId)).thenReturn(currentBalance);

        // when & then
        assertThatThrownBy(() -> userService.useBalance(userId, 50000L))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
        verify(distributedLockRepository, times(1)).tryLock(anyString(), anyString(), anyLong());
        verify(distributedLockRepository, times(1)).unlock(anyString(), anyString());
    }
}