package kr.hhplus.be.server.user.repository;

import kr.hhplus.be.server.user.domain.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

    /**
     * # Method설명 : userId로 최근 잔액내역 1권 조회
     * # MethodName : findTopByUser_UserIdOrderByCreatedAtDesc
     **/
    Optional<UserBalance> findTopByUser_UserIdOrderByCreatedAtDesc(Long userId);

}
