package com.company.auth_service.domain.token;

import java.time.Instant;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiresAt;
    private boolean used;
    private String eventId;
    private boolean notified;
    private Instant createdAt;

    public static PasswordResetToken issue(UUID userId, String token, Instant expiresAt, String eventId) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(userId);
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUsed(false);
        resetToken.setEventId(eventId);
        resetToken.setNotified(false);
        return resetToken;
    }

    public void markNotified() {
        this.notified = true;
    }
}


