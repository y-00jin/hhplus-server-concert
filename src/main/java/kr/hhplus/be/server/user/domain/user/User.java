package kr.hhplus.be.server.user.domain.user;

import java.time.LocalDateTime;

public class User {

    private Long userId;
    private String uuid;
    private String email;
    private String password;
    private String userNm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(Long userId, String uuid, String email, String password, String userNm, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.uuid = uuid;
        this.email = email;
        this.password = password;
        this.userNm = userNm;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String uuid, String email, String password, String userNm) {
        LocalDateTime now = LocalDateTime.now();
        return new User(null, uuid, email, password,userNm,now,now);
    }


    public Long getUserId() {
        return userId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUserNm() {
        return userNm;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void assignId(Long userId) { this.userId = userId; }
}
