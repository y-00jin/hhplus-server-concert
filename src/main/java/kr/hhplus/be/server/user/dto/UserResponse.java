package kr.hhplus.be.server.user.dto;


public record UserResponse(
        Long userId,
        String email,
        String userNm
) {}