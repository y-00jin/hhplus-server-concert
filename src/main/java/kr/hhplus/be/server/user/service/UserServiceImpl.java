package kr.hhplus.be.server.user.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import kr.hhplus.be.server.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.user.dto.UserResponse;
import kr.hhplus.be.server.user.repository.UserBalanceRepository;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
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
        return userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
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
    @Transactional
    @Override
    public UserBalanceResponse chargeBalance(Long userId, UserBalanceRequest request) {

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"));

        // 현재 잔액 조회 (없으면 0)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        // 잔액 충전
        UserBalance chargedBalance = UserBalance.charge(
                User.builder().userId(userId).build(),
                request.getAmount(),
                currentBalance
        );

        UserBalance saved = userBalanceRepository.save(chargedBalance);
        return saved.toResponse();
    }

    /**
     * # Method설명 : 사용자 잔액 사용 (요청 DTO: userId, amount, type)
     * # MethodName : useBalance
     **/
    @Transactional
    @Override
    public UserBalanceResponse useBalance(Long userId, UserBalanceRequest request) {

        long amount = request.getAmount();

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"));

        // 현재 잔액 조회 (없으면 0)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        // 잔액 사용
        UserBalance usedBalance = UserBalance.use(
                User.builder().userId(userId).build(),
                request.getAmount(),
                currentBalance
        );

        UserBalance saved = userBalanceRepository.save(usedBalance);
        return saved.toResponse();
    }

}
