package com.company.auth_service.fixtures;

import com.company.auth_service.domain.user.Roles;
import com.company.auth_service.domain.user.User;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserFixture {

    private UUID id = null;
    private String email = "user-" + UUID.randomUUID() + "@example.com";
    private String passwordHash = "hash";
    private Roles role = Roles.USER;
    private boolean enabled = true;
    private String eventId = UUID.randomUUID().toString();
    private boolean notified = false;

    public static UserFixture getInstance() {
        return new UserFixture();
    }

    public User toUser() {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setEventId(eventId);
        user.setNotified(notified);
        return user;
    }

    public UserEntity toEntity() {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setEmail(email);
        entity.setPasswordHash(passwordHash);
        entity.setRole(role);
        entity.setEnabled(enabled);
        entity.setEventId(eventId);
        entity.setNotified(notified);
        return entity;
    }
}