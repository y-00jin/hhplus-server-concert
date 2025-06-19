package kr.hhplus.be.server.application.user;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {   // 좌석 예약 서비스
    private final UserRepository userRepository;

    private final UserBalanceRepository userBalanceRepository;

    /**
     * # Method설명 : 사용자 조회
     * # MethodName : getUser
     **/
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 사용자 잔액 조회 (가장 최신 내역)
     * # MethodName : getCurrentBalance
     **/
    public UserBalance getCurrentBalance(Long userId) {

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));

        // 잔액 조회
        return userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .orElse(new UserBalance(null, userId, 0L, null, 0L, null, null));
    }

    /**
     * # Method설명 : 사용자 잔액 충전 (요청 DTO: userId, amount, type)
     * # MethodName : chargeBalance
     **/
    @Transactional
    public UserBalance chargeBalance(Long userId, long amount) {

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));

        // 현재 잔액 조회 (없으면 0)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        // 잔액 충전
        UserBalance chargedBalance = UserBalance.charge(
                userId,
                amount,
                currentBalance
        );

        return userBalanceRepository.save(chargedBalance);
    }

    /**
     * # Method설명 : 사용자 잔액 사용
     * # MethodName : useBalance
     **/
    @Transactional
    public UserBalance useBalance(Long userId, long amount) {

        // 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));

        // 현재 잔액 조회 (없으면 0)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        // 잔액 사용
        UserBalance usedBalance = UserBalance.use(
                userId,
                amount,
                currentBalance
        );

        return userBalanceRepository.save(usedBalance);
    }


}
