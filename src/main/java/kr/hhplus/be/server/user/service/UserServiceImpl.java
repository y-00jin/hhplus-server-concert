package kr.hhplus.be.server.user.service;

import kr.hhplus.be.server.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.user.dto.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{


    @Override
    public UserResponse getUser(Long userId) {
        return null;
    }

    @Override
    public UserBalanceResponse getCurrentBalance(Long userId) {
        return null;
    }

    @Override
    public UserBalanceResponse chargeBalance(UserBalanceRequest request) {
        return null;
    }

    @Override
    public UserBalanceResponse useBalance(UserBalanceRequest request) {
        return null;
    }

}
