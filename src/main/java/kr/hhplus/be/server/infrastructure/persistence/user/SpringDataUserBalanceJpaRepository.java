package kr.hhplus.be.server.infrastructure.persistence.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataUserBalanceJpaRepository extends JpaRepository<UserBalanceEntity, Long> {

    /**
     * # Method설명 : userId로 최근 잔액내역 1권 조회
     * # MethodName : findTopByUser_UserIdOrderByBalanceHistoryIdDesc
     **/
    Optional<UserBalanceEntity> findTopByUser_UserIdOrderByBalanceHistoryIdDesc(Long userId);

    /**
     * # Method설명 : 가장 최근 내역 조회 - 비관적 락 적용
     * # MethodName : findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate
     **/
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ub FROM UserBalanceEntity ub WHERE ub.user.userId = :userId ORDER BY ub.balanceHistoryId DESC")
    Optional<UserBalanceEntity> findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(@Param("userId") Long userId);

    /**
     * # Method설명 : 사용자 현재 잔액 조회
     * # MethodName : findCurrentBalanceByUserId
     **/
    @Query("SELECT ub.currentBalance FROM UserBalanceEntity ub WHERE ub.user.userId = :userId ORDER BY ub.balanceHistoryId DESC")
    Optional<Long> findCurrentBalanceByUserId(@Param("userId") Long userId);

}