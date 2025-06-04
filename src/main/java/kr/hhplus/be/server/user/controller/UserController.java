package kr.hhplus.be.server.user.controller;


import kr.hhplus.be.server.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.user.service.UserService;
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
    public ResponseEntity<UserBalanceResponse> getUserBalance(@PathVariable Long userId) {
        UserBalanceResponse response = userService.getCurrentBalance(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * # Method설명 : 사용자 잔액 충전
     * # MethodName : chargeUserBalance
     **/
    @PostMapping("/{userId}/balance/charge")
    public ResponseEntity<UserBalanceResponse> chargeUserBalance(@PathVariable Long userId, @RequestBody UserBalanceRequest request) {
        return ResponseEntity.ok(userService.chargeBalance(userId, request));
    }

    /**
     * # Method설명 : 사용자 포인트 사용
     * # MethodName : useUserBalance
     **/
    @PostMapping("/{userId}/balance/use")
    public ResponseEntity<UserBalanceResponse> useUserBalance(@PathVariable Long userId, @RequestBody UserBalanceRequest request) {
        return ResponseEntity.ok(userService.useBalance(userId, request));
    }
}
