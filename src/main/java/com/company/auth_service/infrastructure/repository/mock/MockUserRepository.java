package com.company.auth_service.infrastructure.repository.mock;

import com.company.auth_service.domain.PendingUserNotification;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Repository
@ConditionalOnProperty(prefix = "auth", name = "repository-mode", havingValue = "mock", matchIfMissing = true)
public class MockUserRepository implements UserRepository {

    private final ConcurrentHashMap<UUID, User> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> byEmail = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(byEmail.get(email)).map(byId::get);
    }

    @Override
    public boolean existsByEmail(String email) {
        return byEmail.containsKey(email);
    }

    @Override
    public Map<UUID, String> findEmailsByIds(Collection<UUID> ids) {
        return Map.of();
    }

    @Override
    public List<PendingUserNotification> lockPendingNotifications(int limit) {
        return List.of();
    }

    @Override
    public int markNotified(Collection<UUID> ids) {
        return 0;
    }

//    @Override
//    public List<User> findAllByNotifiedFalse() {
//        return byId.values().stream()
//                .filter(u -> !u.isNotified())
//                .toList();
//    }

    @Override
    public User save(User user) {
        UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        user.setId(id);
        byId.put(id, user);
        byEmail.put(user.getEmail(), id);
        return user;
    }
}
