package kr.hhplus.be.server.user.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);
    User save(User user);
    boolean existsById(Long userId);

    void deleteAllForTest();
}
