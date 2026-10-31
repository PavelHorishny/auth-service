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
        Map<UUID,String> emails = new HashMap<>();
        for(UUID id : ids){
            User user = byId.get(id);
            if(user != null){
                emails.put(id,user.getEmail());
            }
        }
        return emails;
    }

    @Override
    public List<PendingUserNotification> lockPendingNotifications(int limit) {
        return byId.values().stream()
                .filter(u->!u.isNotified())
                .limit(limit)
                .map(u->new PendingUserNotification(u.getId(),u.getEventId(), u.getEmail()))
                .toList();
    }

    @Override
    public int markNotified(Collection<UUID> ids) {
        int updated = 0;
        for(UUID id:ids){
            User user = byId.get(id);
            if(user != null && !user.isNotified()){
                user.markNotified();
                updated++;
            }
        }
        return updated;
    }

/*    @Override
    public void updatePasswordHash(UUID userId, String passwordHash){
        User user = byId.get(userId);
        if(user == null){
            throw new IllegalStateException("User not found for passwort reset: " + userId);
        }
        user.changePassword(passwordHash);
    }*/

    @Override
    public User save(User user) {
        UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        user.setId(id);
        byId.put(id, user);
        byEmail.put(user.getEmail(), id);
        return user;
    }
}
