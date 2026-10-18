package com.company.auth_service.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;
    private String email;
    private String passwordHash;
    private Roles role;
    private boolean enabled;
    private String eventId;
    private boolean notified;
    private Instant createdAt;
    private Instant updatedAt;

    public void completeRegistration(String passwordHash, String eventId) {
        this.passwordHash = passwordHash;
        this.role = Roles.USER;
        this.enabled = true;
        this.eventId = eventId;
        this.notified = false;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void markNotified() {
        this.notified = true;
    }
}

