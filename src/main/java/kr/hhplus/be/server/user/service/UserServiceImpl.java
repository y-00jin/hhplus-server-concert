package kr.hhplus.be.server.user.service;

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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    private final UserBalanceRepository userBalanceRepository;

    /**
     * # Method설명 : 사용자 조회
     * # MethodName : getUser
     **/
    @Override
    public UserResponse getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"))
                .toResponse();
    }

    /**
     * # Method설명 : 사용자 잔액 조회 (가장 최신 내역)
     * # MethodName : getCurrentBalance
     **/
    @Override
    public UserBalanceResponse getCurrentBalance(Long userId) {

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"));

        // 잔액 조회
        return userBalanceRepository.findTopByUser_UserIdOrderByCreatedAtDesc(userId)
                .map(UserBalance::toResponse)
                .orElse(UserBalanceResponse.builder()
                        .balanceHistoryId(null)
                        .userId(userId)
                        .amount(0L)
                        .type(null)
                        .currentBalance(0L)
                        .description(null)
                        .build()
                );
    }

    /**
     * # Method설명 : 사용자 잔액 충전 (요청 DTO: userId, amount, type)
     * # MethodName : chargeBalance
     **/
    @Override
    public UserBalanceResponse chargeBalance(Long userId, UserBalanceRequest request) {

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"));

        // 0원 이하 충전 불가능
        if(request.getAmount() <= 0)
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "충전 금액은 1원 이상이어야 합니다");

        // 현재 잔액 조회 (없으면 0)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByCreatedAtDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        // 잔액 충전 (현재 잔액 + 충전 잔액)
        UserBalance chargedBalance = UserBalance.builder()
                .user(User.builder().userId(userId).build())
                .amount(request.getAmount())
                .type(UserBalanceType.CHARGE)   // 충전 상태
                .currentBalance(currentBalance + request.getAmount())
                .description(request.getAmount() + "원 " + UserBalanceType.CHARGE.getDescription())  // ~원 충전
                .createdAt(LocalDateTime.now())
                .build();

        UserBalance saved = userBalanceRepository.save(chargedBalance);
        return saved.toResponse();
    }

    /**
     * # Method설명 : 사용자 잔액 사용 (요청 DTO: userId, amount, type)
     * # MethodName : useBalance
     **/
    @Override
    public UserBalanceResponse useBalance(Long userId, UserBalanceRequest request) {

        long amount = request.getAmount();

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"));

        // 0원 이하 사용 불가능
        if(amount <= 0)
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "사용 금액은 1원 이상이어야 합니다");

        // 현재 잔액 조회 (없으면 0)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByCreatedAtDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        // 현재 잔고 < 사용 금액
        if(currentBalance < amount){
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "잔액이 부족합니다");
        }

        // 잔액 사용 (현재 잔액 - 사용 잔액)
        UserBalance usedBalance = UserBalance.builder()
                .user(User.builder().userId(userId).build())
                .amount(amount)
                .type(UserBalanceType.USE)   // 충전 상태
                .currentBalance(currentBalance - amount)
                .description(amount + "원 " + UserBalanceType.USE.getDescription())  // ~원 사용
                .createdAt(LocalDateTime.now())
                .build();

        UserBalance saved = userBalanceRepository.save(usedBalance);
        return saved.toResponse();
    }

}
