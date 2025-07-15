package kr.hhplus.be.server.user.application;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.user.domain.user.User;
import kr.hhplus.be.server.user.domain.user.UserRepository;
import kr.hhplus.be.server.user.domain.userBalance.UserBalance;
import kr.hhplus.be.server.user.domain.userBalance.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Service
public class UserService {   // 좌석 예약 서비스
    private final UserRepository userRepository;

    private final UserBalanceRepository userBalanceRepository;
    private final DistributedLockRepository distributedLockRepository;

    private static final long LOCK_TIMEOUT_MILLIS = 15000;
    private static int MAX_RETRY = 10;   // 최대 시도 수
    private static long SLEEP_MILLIS = 200;  // 대기 시간


    /**
     * # Method설명 : 사용자 조회
     * # MethodName : getUser
     **/
    public User getUser(Long userId) {
        return validateUser(userId);
    }

    /**
     * # Method설명 : 사용자 잔액 조회 (가장 최신 내역)
     * # MethodName : getCurrentBalance
     **/
    public UserBalance getCurrentBalance(Long userId) {

        validateUser(userId);   // 사용자 검증

        // 잔액 조회
        return userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .orElse(new UserBalance(null, userId, 0L, null, 0L, null, null));
    }

    /**
     * # Method설명 : 사용자 잔액 충전
     * # MethodName : chargeBalance
     **/
    public UserBalance chargeBalance(Long userId, long amount) {
        validateUser(userId);
        return withUserBalanceLock(userId, () -> chargeBalanceTransactional(userId, amount));
    }

    /**
     * # Method설명 : 사용자 잔액 사용
     * # MethodName : useBalance
     **/
    public UserBalance useBalance(Long userId, long amount) {
        validateUser(userId);
        return withUserBalanceLock(userId, () -> useBalanceTransactional(userId, amount));
    }

    /**
     * # Method설명 : 사용자 잔액 분산락
     * # MethodName : withUserBalanceLock
     **/
    private UserBalance withUserBalanceLock(Long userId, Supplier<UserBalance> action) {
        String lockKey = "userBalance-lock:" + userId;  // 사용자 id로 사용자 잔액 락
        String lockValue = UUID.randomUUID().toString();
        int tryCount = 0;

        while (true) {
            boolean locked = distributedLockRepository.tryLock(lockKey, lockValue, LOCK_TIMEOUT_MILLIS);
            if (locked) {   // 락 획득
                try {
                    return action.get();    // 트랜잭션 부분 실행
                } finally {
                    distributedLockRepository.unlock(lockKey, lockValue);   // 락 해제
                }
            } else {        // 락 획득 실패
                tryCount++;
                if (tryCount >= MAX_RETRY) {
                    throw new ApiException(ErrorCode.FORBIDDEN, "요청이 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");
                }
                try {
                    Thread.sleep(SLEEP_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "락 대기 중 인터럽트 발생");
                }
            }
        }
    }

    /**
     * # Method설명 : 사용자 userId 검증
     * # MethodName : validateUser
     **/
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID(" + userId + ")의 사용자를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 사용자 잔액 충전 트랜잭션
     * # MethodName : chargeBalanceTransactional
     **/
    @Transactional
    protected UserBalance chargeBalanceTransactional(Long userId, long amount){
        // 현재 잔액 조회
        long currentBalance = userBalanceRepository.findCurrentBalanceByUserId(userId);

        // 잔액 충전
        UserBalance chargedBalance = UserBalance.charge(userId, amount, currentBalance);
        return userBalanceRepository.save(chargedBalance);
    }

    /**
     * # Method설명 : 사용자 잔액 사용 트랜잭션
     * # MethodName : useBalanceTransactional
     **/
    @Transactional
    protected UserBalance useBalanceTransactional(Long userId, long amount){
        // 현재 잔액 조회
        long currentBalance = userBalanceRepository.findCurrentBalanceByUserId(userId);

        // 잔액 사용
        UserBalance usedBalance = UserBalance.use(userId, amount, currentBalance);
        return userBalanceRepository.save(usedBalance);
    }


}
