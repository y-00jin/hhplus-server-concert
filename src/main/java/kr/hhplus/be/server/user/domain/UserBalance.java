package kr.hhplus.be.server.user.domain;


import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.user.domain.enums.UserBalanceType;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_balance_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_history_id")
    private Long balanceHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private UserBalanceType type;

    @Column(name = "current_balance", nullable = false)
    private long currentBalance;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP", updatable = false, insertable = false)
    private LocalDateTime createdAt;


    public UserBalanceResponse toResponse(){
        return UserBalanceResponse.builder()
                .balanceHistoryId(this.balanceHistoryId)
                .userId(this.user.getUserId())
                .amount(this.amount)
                .type(this.type)
                .currentBalance(this.currentBalance)
                .description(this.description)
                .build();
    }

    /**
     * # Method설명 : 잔액 충전 - UserBalance 검증 및 생성
     * # MethodName : charge
     **/
    public static UserBalance charge(User user, long amount, long currentBalance) {
        // 0원 이하 충전 불가능
        if (amount <= 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "충전 금액은 1원 이상이어야 합니다");
        }
        return UserBalance.builder()
                .user(user)
                .amount(amount)
                .type(UserBalanceType.CHARGE)
                .currentBalance(currentBalance + amount)
                .description(amount +"원 " + UserBalanceType.CHARGE.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * # Method설명 : 잔액 사용 - UserBalance 검증 및 생성
     * # MethodName : use
     **/
    public static UserBalance use(User user, long amount, long currentBalance) {
        // 0원 이하 사용 불가능
        if(amount <= 0)
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "사용 금액은 1원 이상이어야 합니다");

        // 현재 잔고 < 사용 금액
        if(currentBalance < amount){
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "잔액이 부족합니다");
        }

        return UserBalance.builder()
                .user(user)
                .amount(amount)
                .type(UserBalanceType.USE)
                .currentBalance(currentBalance - amount)
                .description(amount +"원 " + UserBalanceType.USE.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
    }

}
