package kr.hhplus.be.server.domain.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void 생성자_정상_생성() {
        // given
        Long userId = 1L;
        String uuid = "uuid";
        String email = "test@test.com";
        String password = "pw";
        String userNm = "테스트";
        LocalDateTime now = LocalDateTime.now();

        // when
        User user = new User(userId, uuid, email, password, userNm, now, now);

        // then
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getUuid()).isEqualTo(uuid);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getUserNm()).isEqualTo(userNm);
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void create_정적_팩토리_정상_생성() {
        // given
        String uuid = "uuid";
        String email = "test2@test.com";
        String password = "pw2";
        String userNm = "유저";

        // when
        User user = User.create(uuid, email, password, userNm);

        // then
        assertThat(user.getUserId()).isNull();
        assertThat(user.getUuid()).isEqualTo(uuid);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getUserNm()).isEqualTo(userNm);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
    }

    @Test
    void assignId_정상_동작() {
        // given
        User user = User.create("uuid", "test@test.com", "pw", "테스터");

        // when
        user.assignId(99L);

        // then
        assertThat(user.getUserId()).isEqualTo(99L);
    }
}