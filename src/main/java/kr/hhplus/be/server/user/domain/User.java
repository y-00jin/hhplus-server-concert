package kr.hhplus.be.server.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "uuid", columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private byte[] uuid;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "user_nm", length = 50, nullable = false)
    private String userNm;

    @Column(name = "created_at", columnDefinition = "DATETIME", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

}
