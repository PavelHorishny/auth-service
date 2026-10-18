package com.company.auth_service.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByNotifiedFalse();
    User save(User user);
}
