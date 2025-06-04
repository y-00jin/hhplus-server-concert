package kr.hhplus.be.server.user.domain;


import jakarta.persistence.*;
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

    @Column(name = "created_at", columnDefinition = "DATETIME", updatable = false, insertable = false)
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

}
