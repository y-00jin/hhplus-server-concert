package kr.hhplus.be.server.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.user.domain.enums.UserBalanceType;
import kr.hhplus.be.server.user.dto.UserBalanceRequest;
import kr.hhplus.be.server.user.dto.UserBalanceResponse;
import kr.hhplus.be.server.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId = 1L;
    private long chargeAmount = 10000L;
    private String userApi = "/api/v1/users/" + userId;


    @Test
    @DisplayName("사용자 잔액 조회 성공")
    void getUserBalance() throws Exception {
        // given
        UserBalanceResponse response = UserBalanceResponse.builder()
                .userId(userId)
                .currentBalance(chargeAmount)
                .build();

        when(userService.getCurrentBalance(anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(get(userApi +"/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.currentBalance").value(chargeAmount));
    }

    @Test
    @DisplayName("사용자 잔액 조회 실패 - 존재하지 않는 사용자")
    void getUserBalance_NotFound() throws Exception {
        // given
        Mockito.when(userService.getCurrentBalance(anyLong()))
                .thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(get(userApi + "/balance"))
                .andExpect(status().isNotFound()) // 404 체크
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.msg").value("해당 ID의 사용자를 찾을 수 없습니다"));
    }


    @Test
    @DisplayName("사용자 잔액 충전 성공")
    void chargeUserBalance() throws Exception {
        // given
        UserBalanceRequest request = UserBalanceRequest.builder()
                .amount(chargeAmount)
                .type(UserBalanceType.CHARGE)
                .build();

        UserBalanceResponse response = UserBalanceResponse.builder()
                .userId(userId)
                .currentBalance(chargeAmount)
                .build();

        Mockito.when(userService.chargeBalance(anyLong(), any(UserBalanceRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post(userApi +"/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.currentBalance").value(chargeAmount));
    }

    @Test
    @DisplayName("사용자 잔액 충전 실패 - 0원 이하 충전")
    void chargeUserBalance_InvalidAmount() throws Exception {
        // given
        UserBalanceRequest request = UserBalanceRequest.builder()
                .amount(0L)
                .type(UserBalanceType.CHARGE)
                .build();

        Mockito.when(userService.chargeBalance(anyLong(), any(UserBalanceRequest.class)))
                .thenThrow(new ApiException(ErrorCode.INVALID_INPUT_VALUE, "충전 금액은 1원 이상이어야 합니다"));

        // when & then
        mockMvc.perform(post(userApi + "/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400 에러
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.msg").value("충전 금액은 1원 이상이어야 합니다"));
    }


    @Test
    @DisplayName("사용자 포인트 사용 성공")
    void useUserBalance() throws Exception {
        long useAmount = 3000L;
        // given
        UserBalanceRequest request = UserBalanceRequest.builder()
                .amount(useAmount)
                .type(UserBalanceType.USE)
                .build();

        UserBalanceResponse response = UserBalanceResponse.builder()
                .userId(userId)
                .currentBalance(chargeAmount - useAmount)
                .build();

        Mockito.when(userService.useBalance(anyLong(), any(UserBalanceRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post(userApi +"/balance/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.currentBalance").value(chargeAmount - useAmount));
    }


    @Test
    @DisplayName("사용자 포인트 사용 실패 - 잔액 부족")
    void useUserBalance_InsufficientBalance() throws Exception {
        long useAmount = 100_000L; // 현재 잔액보다 훨씬 크게
        UserBalanceRequest request = UserBalanceRequest.builder()
                .amount(useAmount)
                .type(UserBalanceType.USE)
                .build();

        Mockito.when(userService.useBalance(anyLong(), any(UserBalanceRequest.class)))
                .thenThrow(new ApiException(ErrorCode.INVALID_INPUT_VALUE, "잔액이 부족합니다"));

        mockMvc.perform(post(userApi + "/balance/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400 에러
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.msg").value("잔액이 부족합니다"));
    }


}
