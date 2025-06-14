package kr.hhplus.be.server.api.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse{

    private Long userId;
    private String email;
    private String userNm;

}