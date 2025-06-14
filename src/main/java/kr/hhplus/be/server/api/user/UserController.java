package kr.hhplus.be.server.api.user;


import kr.hhplus.be.server.api.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.api.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.user.UserBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * # Method설명 : 사용자 잔액 조회
     * # MethodName : getUserBalance
     **/
    @GetMapping("/{userId}/balance")
    public ResponseEntity<UserBalanceResponse> getUserBalance(@PathVariable("userId") Long userId) {
        UserBalance userBalance = userService.getCurrentBalance(userId);
        return ResponseEntity.ok(toResponse(userBalance));
    }

    /**
     * # Method설명 : 사용자 잔액 충전
     * # MethodName : chargeUserBalance
     **/
    @PostMapping("/{userId}/balance/charge")
    public ResponseEntity<UserBalanceResponse> chargeUserBalance(@PathVariable("userId") Long userId, @RequestBody UserBalanceRequest request) {
        UserBalance userBalance = userService.chargeBalance(userId, request.getAmount());
        return ResponseEntity.ok(toResponse(userBalance));
    }

    /**
     * # Method설명 : 사용자 포인트 사용
     * # MethodName : useUserBalance
     **/
    @PostMapping("/{userId}/balance/use")
    public ResponseEntity<UserBalanceResponse> useUserBalance(@PathVariable("userId") Long userId, @RequestBody UserBalanceRequest request) {
        UserBalance userBalance = userService.useBalance(userId, request.getAmount());
        return ResponseEntity.ok(toResponse(userBalance));
    }

    private UserBalanceResponse toResponse(UserBalance userBalance){
        return UserBalanceResponse.builder()
                .balanceHistoryId(userBalance.getBalanceHistoryId())
                .userId(userBalance.getUserId())
                .amount(userBalance.getAmount())
                .type(userBalance.getType())
                .currentBalance(userBalance.getCurrentBalance())
                .description(userBalance.getDescription())
                .build();
    }
}
