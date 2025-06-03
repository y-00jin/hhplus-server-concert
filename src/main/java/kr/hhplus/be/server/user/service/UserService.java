package kr.hhplus.be.server.user.service;

import kr.hhplus.be.server.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.user.dto.UserResponse;

public interface UserService {

    /**
     * # Method설명 : 사용자 조회
     * # MethodName : getUser
     **/
    UserResponse getUser(Long userId);

    /**
     * # Method설명 : 사용자 잔액 조회 (가장 최신 내역)
     * # MethodName : getCurrentBalance
     **/
    UserBalanceResponse getCurrentBalance(Long userId);

    /**
     * # Method설명 : 사용자 잔액 충전 (요청 DTO: userId, amount, type, description)
     * # MethodName : chargeBalance
     **/
    UserBalanceResponse chargeBalance(UserBalanceRequest request);


    /**
     * # Method설명 : 사용자 잔액 사용 (요청 DTO: userId, amount, type, description)
     * # MethodName : useBalance
     **/
    UserBalanceResponse useBalance(UserBalanceRequest request);
}
