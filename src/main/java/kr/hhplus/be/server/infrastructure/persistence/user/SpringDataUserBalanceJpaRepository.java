package kr.hhplus.be.server.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserBalanceJpaRepository extends JpaRepository<UserBalanceEntity, Long> {

    /**
     * # Method설명 : userId로 최근 잔액내역 1권 조회
     * # MethodName : findTopByUser_UserIdOrderByBalanceHistoryIdDesc
     **/
    Optional<UserBalanceEntity> findTopByUser_UserIdOrderByBalanceHistoryIdDesc(Long userId);

}