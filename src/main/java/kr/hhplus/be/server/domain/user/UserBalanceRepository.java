package kr.hhplus.be.server.domain.user;


import java.util.Optional;

public interface UserBalanceRepository {
    /**
     * # Method설명 : userId로 최근 잔액내역 1권 조회
     * # MethodName : findTopByUser_UserIdOrderByBalanceHistoryIdDesc
     **/
    Optional<UserBalance> findTopByUser_UserIdOrderByBalanceHistoryIdDesc(Long userId);

    UserBalance save(UserBalance userBalance);
}
