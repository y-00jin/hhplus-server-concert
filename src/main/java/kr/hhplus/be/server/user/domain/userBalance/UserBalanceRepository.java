package kr.hhplus.be.server.user.domain.userBalance;


import java.util.Optional;

public interface UserBalanceRepository {
    /**
     * # Method설명 : userId로 최근 잔액내역 1권 조회
     * # MethodName : findTopByUser_UserIdOrderByBalanceHistoryIdDesc
     **/
    Optional<UserBalance> findTopByUser_UserIdOrderByBalanceHistoryIdDesc(Long userId);

    /**
     * # Method설명 : userId로 최근 잔액내역 1권 조회 (비관적 락 적용)
     * # MethodName : findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate
     **/
    Optional<UserBalance> findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(Long userId);

    /**
     * # Method설명 : 사용자 현재 잔액 조회
     * # MethodName : findCurrentBalanceByUserId
     **/
    Long findCurrentBalanceByUserId(Long userId);

    /**
     * # Method설명 : 잔액내역 저장
     * # MethodName : save
     **/
    UserBalance save(UserBalance userBalance);

    void deleteAllForTest();
}
