package kr.hhplus.be.server.infrastructure.persistence.user;


import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserJpaRepository implements UserRepository {

    private final SpringDataUserJpaRepository jpaRepository;

    public UserJpaRepository(SpringDataUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(Long userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }


    private UserEntity toEntity(User domain) {

        return UserEntity.builder()
                .userId(domain.getUserId())
                .uuid(domain.getUuid())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .userNm(domain.getUserNm())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private User toDomain(UserEntity entity) {
        return new User(
                entity.getUserId(),
                entity.getUuid(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getUserNm(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
