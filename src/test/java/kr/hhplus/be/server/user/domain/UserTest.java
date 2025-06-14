package kr.hhplus.be.server.user.domain;


import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class UserTest {

    @Test
    void userToResponse_정상() {
        User user = User.builder()
                .userId(10L)
                .uuid(new byte[16])
                .email("hello@a.com")
                .password("pw")
                .userNm("테스터")
                .build();

        var dto = user.toResponse();

        assertThat(dto.getUserId()).isEqualTo(10L);
        assertThat(dto.getEmail()).isEqualTo("hello@a.com");
        assertThat(dto.getUserNm()).isEqualTo("테스터");
    }
}