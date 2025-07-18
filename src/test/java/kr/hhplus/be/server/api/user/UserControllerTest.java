package kr.hhplus.be.server.api.user;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.user.api.UserController;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.userBalance.UserBalance;
import kr.hhplus.be.server.user.domain.userBalance.UserBalanceType;
import kr.hhplus.be.server.user.dto.request.UserBalanceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Autowired
    ObjectMapper objectMapper;

    private final Long userId = 1L;

    @Test
    @DisplayName("사용자 잔액 조회 성공")
    void getUserBalance_success() throws Exception {
        // given
        UserBalance userBalance = new UserBalance(1L, userId, 0L, UserBalanceType.CHARGE, 10000L, "잔액조회", null);
        when(userService.getCurrentBalance(userId)).thenReturn(userBalance);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.currentBalance", is(10000)));
    }

    @Test
    @DisplayName("사용자 잔액 조회 실패 - 존재하지 않는 사용자")
    void getUserBalance_fail_not_found() throws Exception {
        // given
        when(userService.getCurrentBalance(userId))
                .thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND, null));

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/balance", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("사용자 잔액 충전 성공")
    void chargeUserBalance_success() throws Exception {
        // given
        UserBalanceRequest request = new UserBalanceRequest(5000L);
        UserBalance charged = new UserBalance(2L, userId, 5000L, UserBalanceType.CHARGE, 15000L, "충전", null);

        when(userService.chargeBalance(userId, request.getAmount())).thenReturn(charged);

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.currentBalance", is(15000)))
                .andExpect(jsonPath("$.amount", is(5000)))
                .andExpect(jsonPath("$.type", is("CHARGE")));
    }

    @Test
    @DisplayName("사용자 잔액 충전 실패 - 0원 이하 충전")
    void chargeUserBalance_fail_zeroOrNegative() throws Exception {
        // given
        UserBalanceRequest request = new UserBalanceRequest(0L);
        when(userService.chargeBalance(userId, request.getAmount()))
                .thenThrow(new ApiException(ErrorCode.INVALID_INPUT_VALUE, null));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 포인트 사용 성공")
    void useUserBalance_success() throws Exception {
        // given
        UserBalanceRequest request = new UserBalanceRequest(3000L);
        UserBalance used = new UserBalance(3L, userId, 3000L, UserBalanceType.USE, 12000L, "사용", null);

        when(userService.useBalance(userId, request.getAmount())).thenReturn(used);

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.currentBalance", is(12000)))
                .andExpect(jsonPath("$.amount", is(3000)))
                .andExpect(jsonPath("$.type", is("USE")));
    }

    @Test
    @DisplayName("사용자 포인트 사용 실패 - 잔액 부족")
    void useUserBalance_fail_notEnough() throws Exception {
        // given
        UserBalanceRequest request = new UserBalanceRequest(50000L);
        when(userService.useBalance(userId, request.getAmount()))
                .thenThrow(new ApiException(ErrorCode.INVALID_INPUT_VALUE, null));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}