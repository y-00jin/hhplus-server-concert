package kr.hhplus.be.server.infrastructure.persistence.user;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserBalanceJpaRepository implements UserBalanceRepository {

    private final SpringDataUserBalanceJpaRepository userBalanceRepository;
    private final SpringDataUserJpaRepository userRepository;

    public UserBalanceJpaRepository(SpringDataUserBalanceJpaRepository userBalanceRepository, SpringDataUserJpaRepository userRepository) {
        this.userBalanceRepository = userBalanceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserBalance> findTopByUser_UserIdOrderByBalanceHistoryIdDesc(Long userId) {
        return userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserBalance> findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(Long userId) {
        return userBalanceRepository.findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId).map(this::toDomain);
    }

    @Override
    public UserBalance save(UserBalance userBalance) {
        UserBalanceEntity saved = userBalanceRepository.save(toEntity(userBalance));
        return toDomain(saved);
    }

    @Override
    public void deleteAllForTest() {
        userBalanceRepository.deleteAll();
    }


    private UserBalanceEntity toEntity(UserBalance domain) {
        // 1. id로 User, Seat 객체 조회
        UserEntity user = userRepository.findById(domain.getUserId()).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));
        return UserBalanceEntity.builder()
                .balanceHistoryId(domain.getBalanceHistoryId())
                .user(user)
                .amount(domain.getAmount())
                .type(domain.getType())
                .currentBalance(domain.getCurrentBalance())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private UserBalance toDomain(UserBalanceEntity entity) {
        return new UserBalance(
                entity.getBalanceHistoryId(),
                entity.getUser().getUserId(),
                entity.getAmount(),
                entity.getType(),
                entity.getCurrentBalance(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

}
