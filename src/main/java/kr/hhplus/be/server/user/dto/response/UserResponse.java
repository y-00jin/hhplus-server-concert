package kr.hhplus.be.server.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse{

    private Long userId;
    private String email;
    private String userNm;

}