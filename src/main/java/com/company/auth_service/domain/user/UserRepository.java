package com.company.auth_service.domain.user;

import java.util.*;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Map<UUID, String> findEmailsByIds(Collection<UUID> ids);
    List<PendingUserNotification> lockPendingNotifications(int limit);
    int markNotified(Collection<UUID> ids);
    User save(User user);
    void updatePasswordHash(UUID userId, String passwordHash);
}
