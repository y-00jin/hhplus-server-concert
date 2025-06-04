package kr.hhplus.be.server.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import kr.hhplus.be.server.user.domain.enums.UserBalanceType;
import kr.hhplus.be.server.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.user.dto.UserResponse;
import kr.hhplus.be.server.user.repository.UserBalanceRepository;
import kr.hhplus.be.server.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    UserServiceImpl userService;

    @Mock
    UserRepository userRepository;

    @Mock
    UserBalanceRepository userBalanceRepository;

    private Long userId = 1L;
    private User userEntity;
    private long chargeAmount = 5000L;

    @BeforeEach
    void beforeEach() {
        // 사용자 정보
        userEntity = User.builder()
                .userId(userId)
                .uuid(new byte[16])
                .email("test@naver.com")
                .password("1234")
                .userNm("홍길동")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    /**
     * # Method설명 : UserBalance 객체 생성
     * # MethodName : createUserBalance
     **/
    private UserBalance createUserBalance(Long balanceHistoryId, long amount, UserBalanceType type, long currentBalance) {
        return UserBalance.builder()
                .balanceHistoryId(balanceHistoryId)
                .user(userEntity)
                .amount(amount)
                .type(type)
                .currentBalance(currentBalance)
                .description(type == null ? null : amount + "원 " + type.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * # Method설명 : UserBalanceRequest 객체 생성
     * # MethodName : createUserBalanceRequest
     **/
    UserBalanceRequest createUserBalanceRequest(long amount, UserBalanceType type){
        return UserBalanceRequest.builder()
                .amount(amount)
                .type(type)
                .build();
    }

    /**
     * # Method설명 : userId에 해당하는 사용자 조회 성공
     * # MethodName : 사용자아이디로_사용자_조회_성공
     **/
    @Test
    void 사용자아이디로_사용자_조회_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        // when
        UserResponse user = userService.getUser(userId);

        // then
        verify(userRepository, times(1)).findById(userId);
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getEmail()).isEqualTo(userEntity.getEmail());
        assertThat(user.getUserNm()).isEqualTo(userEntity.getUserNm());
    }

    /**
     * # Method설명 : userID에 해당하는 사용자 조회 실패
     * # MethodName : 사용자아이디로_사용자_조회_실패
     **/
    @Test
    void 사용자아이디로_사용자_조회_실패(){
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        ApiException ex = catchThrowableOfType(() -> userService.getUser(userId), ApiException.class);

        // then
        verify(userRepository, times(1)).findById(userId);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void 사용자아이디로_잔액_조회_성공(){
        //given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        UserBalance userBalance = createUserBalance(1L, chargeAmount, UserBalanceType.CHARGE, chargeAmount);
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(userBalance));

        // when
        UserBalanceResponse userBalanceRes = userService.getCurrentBalance(userId);

        // then
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);

        assertThat(userBalanceRes).isNotNull();
        assertThat(userBalanceRes.getBalanceHistoryId()).isEqualTo(userBalance.getBalanceHistoryId());
        assertThat(userBalanceRes.getCurrentBalance()).isEqualTo(userBalance.getCurrentBalance());
    }


    /**
     * # Method설명 : userID에 해당하는 잔액 내역이 없을 때 0원을 반환
     * # MethodName : 사용자아이디로_잔액_조회_실패_0원_반환
     **/
    @Test
    void 사용자아이디로_잔액_조회_실패_0원_반환() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        // when
        UserBalanceResponse userBalanceRes = userService.getCurrentBalance(userId);

        // then
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);

        assertThat(userBalanceRes).isNotNull();
        assertThat(userBalanceRes.getCurrentBalance()).isEqualTo(0L);
    }

    /**
     * # Method설명 : 사용자 잔액 성공
     * # MethodName : 사용자아이디로_잔액_충전_성공
     **/
    @Test
    void 사용자아이디로_잔액_충전_성공() {
        // given
        UserBalance userBalance = createUserBalance(null, 0, null, 0);   // 기존 잔액
        UserBalance charged = createUserBalance(1L, chargeAmount, UserBalanceType.CHARGE, chargeAmount); // 충전 잔액
        UserBalanceRequest chargedReq = createUserBalanceRequest(chargeAmount, UserBalanceType.CHARGE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(userBalance));
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(charged);

        // when
        UserBalanceResponse userBalanceRes = userService.chargeBalance(userId, chargedReq); // 충전

        // then
        verify(userRepository, times(1)).findById(userId);
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));

        assertThat(userBalanceRes).isNotNull();
        assertThat(userBalanceRes.getAmount()).isEqualTo(chargeAmount);           // 충전 금액
        assertThat(userBalanceRes.getCurrentBalance()).isEqualTo(chargeAmount);   // 현재 잔액
        assertThat(userBalanceRes.getType()).isEqualTo(UserBalanceType.CHARGE);

    }

    /**
     * # Method설명 : 사용자 잔액 충전 실패 - 사용자 없음
     * # MethodName : 사용자아이디로_잔액_충전_실패_사용자없음
     **/
    @Test
    void 사용자아이디로_잔액_충전_실패_사용자없음() {
        // given: 해당 userId가 없음
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserBalanceRequest chargedReq = createUserBalanceRequest(chargeAmount, UserBalanceType.CHARGE);

        // when & then
        ApiException ex = catchThrowableOfType(() -> userService.chargeBalance(userId, chargedReq), ApiException.class);

        verify(userRepository, times(1)).findById(userId);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * # Method설명 : 사용자 잔액 충전 실패 - 0원 이하 충전
     * # MethodName : 사용자아이디로_잔액_충전_실패_최소금액미만
     **/
    @Test
    void 사용자아이디로_잔액_충전_실패_최소금액미만() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        long chargeAmountZero = 0L;
        UserBalanceRequest chargedReq = createUserBalanceRequest(chargeAmountZero, UserBalanceType.CHARGE);

        // when & then
        ApiException ex = catchThrowableOfType(() -> userService.chargeBalance(userId, chargedReq), ApiException.class);
        verify(userRepository, times(1)).findById(userId);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }


    /**
     * # Method설명 : 포인트 사용 성공
     * # MethodName : 사용자아이디로_포인트_사용_성공
     **/
    @Test
    void 사용자아이디로_포인트_사용_성공() {
        // given
        long useAmount = 3000L; // 사용할 포인트
        UserBalance charged = createUserBalance(1L, chargeAmount, UserBalanceType.CHARGE, chargeAmount);
        UserBalance afterUse = createUserBalance(2L, useAmount, UserBalanceType.USE, chargeAmount - useAmount);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(charged));  // 충전 된 상태의 기존 잔액
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(afterUse);   // 사용 후 잔액

        // when
        UserBalanceRequest useRequest = createUserBalanceRequest(useAmount, UserBalanceType.USE);
        UserBalanceResponse userBalance = userService.useBalance(userId, useRequest);

        // then
        verify(userRepository, times(1)).findById(userId);
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));

        assertThat(userBalance).isNotNull();
        assertThat(userBalance.getCurrentBalance()).isEqualTo(chargeAmount - useAmount);   // 현재 잔액
        assertThat(userBalance.getAmount()).isEqualTo(useAmount);  // 사용한 금액
        assertThat(userBalance.getType()).isEqualTo(UserBalanceType.USE);
    }


    /**
     * # Method설명 : 포인트 사용 실패 - 잔액 부족
     * # MethodName : 사용자아이디로_포인트_사용_실패_잔액부족
     **/
    @Test
    void 사용자아이디로_포인트_사용_실패_잔액부족() {
        // given
        UserBalance charged = createUserBalance(1L, chargeAmount, UserBalanceType.CHARGE, chargeAmount);

        long useAmount = 6000L; // 사용액 > 보유 잔액
        UserBalanceRequest useRequest = createUserBalanceRequest(useAmount, UserBalanceType.USE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(charged));

        // when & then
        ApiException ex = catchThrowableOfType(() -> userService.useBalance(userId, useRequest), ApiException.class);
        verify(userRepository, times(1)).findById(userId);
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * # Method설명 : 포인트 사용 실패 - 최소 금액 미만 (0원 이하 사용)
     * # MethodName : 사용자아이디로_포인트_사용_실패_최소금액미만
     **/
    @Test
    void 사용자아이디로_포인트_사용_실패_최소금액미만() {
        // given
        long useAmount = 0L;
        UserBalanceRequest useRequest = createUserBalanceRequest(useAmount, UserBalanceType.USE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        // when & then
        ApiException ex = catchThrowableOfType(() -> userService.useBalance(userId, useRequest), ApiException.class);
        verify(userRepository, times(1)).findById(userId);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
