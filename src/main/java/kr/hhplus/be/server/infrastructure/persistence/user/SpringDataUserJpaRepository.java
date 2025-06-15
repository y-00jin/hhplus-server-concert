package kr.hhplus.be.server.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, Long> {
}