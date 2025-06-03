package kr.hhplus.be.server.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import jakarta.persistence.EntityNotFoundException;
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

    private Long userId;
    private User userEntity;
    private UserBalance userBalanceEntity;
    private UserBalance chargedUserBalanceEntity;
    UserBalanceRequest chargedUserBalanceRequest;  // 충전 req
    long chargeAmount;  // 충전 금액

    @BeforeEach
    void beforeEach() {

        // 사용자 정보
        userId = 1L;
        userEntity = User.builder()
                .userId(userId)
                .uuid(new byte[16])
                .email("test@naver.com")
                .password("1234")
                .userNm("홍길동")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 충전 전 사용자 잔액
        userBalanceEntity = UserBalance.builder()
                .balanceHistoryId(1L)
                .user(userEntity)
                .type(null)
                .amount(0)
                .description(null)
                .currentBalance(0)
                .createdAt(LocalDateTime.now())
                .build();

        // 충전 후 사용자 잔액
        chargeAmount = 5000L;
        chargedUserBalanceRequest = new UserBalanceRequest(userId, chargeAmount, UserBalanceType.CHARGE, chargeAmount + "원 충전");
        chargedUserBalanceEntity = UserBalance.builder()
                .balanceHistoryId(2L)
                .user(userEntity)
                .type(UserBalanceType.CHARGE)
                .amount(chargeAmount)
                .description(chargeAmount + "원 충전")
                .currentBalance(chargeAmount)
                .createdAt(LocalDateTime.now())
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
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo(userEntity.getEmail());
        assertThat(user.userNm()).isEqualTo(userEntity.getUserNm());
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
        assertThrows(EntityNotFoundException.class, () -> userService.getUser(userId));

        // then
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void 사용자아이디로_잔액_조회_성공(){

        //given
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(userBalanceEntity));

        // when
        UserBalanceResponse userBalance = userService.getCurrentBalance(userId);

        // then
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);

        assertThat(userBalance).isNotNull();
        assertThat(userBalance.balanceHistoryId()).isEqualTo(userBalanceEntity.getBalanceHistoryId());
        assertThat(userBalance.currentBalance()).isEqualTo(userBalanceEntity.getCurrentBalance());
    }


    /**
     * # Method설명 : userID에 해당하는 잔액 내역이 없을 때 0원을 반환
     * # MethodName : 사용자아이디로_잔액_조회_실패_0원_반환
     **/
    @Test
    void 사용자아이디로_잔액_조회_실패_0원_반환() {
        // given

        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        // when
        UserBalanceResponse userBalance = userService.getCurrentBalance(userId);

        // then
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);

        assertThat(userBalance).isNotNull();
        assertThat(userBalance.currentBalance()).isEqualTo(0L);
    }

    /**
     * # Method설명 : 사용자 잔액 성공
     * # MethodName : 사용자아이디로_잔액_충전_성공
     **/
    @Test
    void 사용자아이디로_잔액_충전_성공() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(userBalanceEntity));    // 기존 잔액

        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(chargedUserBalanceEntity);  // 충전 후 잔액

        // when
        UserBalanceResponse userBalance = userService.chargeBalance(chargedUserBalanceRequest); // 충전

        // then
        verify(userRepository, times(1)).findById(userId);
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));

        assertThat(userBalance).isNotNull();
        assertThat(userBalance.amount()).isEqualTo(chargeAmount);           // 충전 금액
        assertThat(userBalance.currentBalance()).isEqualTo(chargeAmount);   // 현재 잔액
        assertThat(userBalance.type()).isEqualTo(UserBalanceType.CHARGE);

    }

    /**
     * # Method설명 : 사용자 잔액 충전 실패 - 사용자 없음
     * # MethodName : 사용자아이디로_잔액_충전_실패_사용자없음
     **/
    @Test
    void 사용자아이디로_잔액_충전_실패_사용자없음() {
        // given: 해당 userId가 없음
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(EntityNotFoundException.class, () -> userService.chargeBalance(chargedUserBalanceRequest));
        verify(userRepository, times(1)).findById(userId);
    }

    /**
     * # Method설명 : 사용자 잔액 충전 실패 - 0원 이하 충전
     * # MethodName : 사용자아이디로_잔액_충전_실패_최소금액미만
     **/
    @Test
    void 사용자아이디로_잔액_충전_실패_최소금액미만() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        long chargeAmount = 0L;
        UserBalanceRequest userBalanceRequest = new UserBalanceRequest(userId, chargeAmount, UserBalanceType.CHARGE, chargeAmount + "원 충전");

        // when & then
        assertThrows(IllegalArgumentException.class, () -> userService.chargeBalance(userBalanceRequest));
    }


    /**
     * # Method설명 : 포인트 사용 성공
     * # MethodName : 사용자아이디로_포인트_사용_성공
     **/
    @Test
    void 사용자아이디로_포인트_사용_성공() {
        // given
        long useAmount = 3000L; // 사용할 포인트
        UserBalance userBalanceAfterUse = UserBalance.builder()
                .balanceHistoryId(2L)
                .user(userEntity)
                .type(UserBalanceType.USE)
                .amount(useAmount)
                .description(useAmount + "원 사용")
                .currentBalance(2000L) // 5000 - 3000
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(chargedUserBalanceEntity)); // 충전 된 상태의 기존 잔액
        when(userBalanceRepository.save(any(UserBalance.class))).thenReturn(userBalanceAfterUse);   // 사용 후 잔액

        // when
        UserBalanceRequest useRequest = new UserBalanceRequest(userId, useAmount, UserBalanceType.USE, useAmount + "원 사용");
        UserBalanceResponse userBalance = userService.useBalance(useRequest);

        // then
        verify(userRepository, times(1)).findById(userId);
        verify(userBalanceRepository, times(1)).findTopByUser_UserIdOrderByCreatedAtDesc(userId);
        verify(userBalanceRepository, times(1)).save(any(UserBalance.class));

        assertThat(userBalance).isNotNull();
        assertThat(userBalance.currentBalance()).isEqualTo(chargeAmount - useAmount);   // 현재 잔액
        assertThat(userBalance.amount()).isEqualTo(useAmount);  // 사용한 금액
        assertThat(userBalance.type()).isEqualTo(UserBalanceType.USE);
    }


    /**
     * # Method설명 : 포인트 사용 실패 - 잔액 부족
     * # MethodName : 사용자아이디로_포인트_사용_실패_잔액부족
     **/
    @Test
    void 사용자아이디로_포인트_사용_실패_잔액부족() {
        // given
        long useAmount = 6000L; // 사용액 > 보유 잔액
        UserBalanceRequest useRequest = new UserBalanceRequest(userId, useAmount, UserBalanceType.USE, useAmount + "원 사용");

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(chargedUserBalanceEntity));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> userService.useBalance(useRequest));
    }

    /**
     * # Method설명 : 포인트 사용 실패 - 최소 금액 미만 (0원 이하 사용)
     * # MethodName : 사용자아이디로_포인트_사용_실패_최소금액미만
     **/
    @Test
    void 사용자아이디로_포인트_사용_실패_최소금액미만() {
        // given
        long useAmount = 0L;
        UserBalanceRequest userBalanceRequest = new UserBalanceRequest(userId, useAmount, UserBalanceType.USE, useAmount + "원 사용");
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> userService.useBalance(userBalanceRequest));
    }
}
