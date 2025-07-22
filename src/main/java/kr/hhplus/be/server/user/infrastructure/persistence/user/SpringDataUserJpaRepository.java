package kr.hhplus.be.server.user.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, Long> {
}