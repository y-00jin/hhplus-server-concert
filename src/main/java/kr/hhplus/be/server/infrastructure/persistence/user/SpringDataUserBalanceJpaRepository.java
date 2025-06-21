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

    // 가장 최근 내역을 "for update"로 select
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ub FROM UserBalanceEntity ub WHERE ub.user.userId = :userId ORDER BY ub.balanceHistoryId DESC")
    Optional<UserBalanceEntity> findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(@Param("userId") Long userId);

}