package com.company.auth_service.fixtures;

import com.company.auth_service.domain.token.PasswordResetToken;
import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PasswordResetTokenFixture {

    private UUID id = null;
    private UUID userId = UUID.randomUUID();     // в IT подставь id реального пользователя (FK)
    private String token = UUID.randomUUID().toString();
    private Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);
    private boolean used = false;
    private String eventId = UUID.randomUUID().toString();
    private boolean notified = false;

    public static PasswordResetTokenFixture getInstance() {
        return new PasswordResetTokenFixture();
    }

    public PasswordResetToken toToken() {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(id);
        t.setUserId(userId);
        t.setToken(token);
        t.setExpiresAt(expiresAt);
        t.setUsed(used);
        t.setEventId(eventId);
        t.setNotified(notified);
        return t;
    }

    public PasswordResetTokenEntity toEntity() {
        PasswordResetTokenEntity e = new PasswordResetTokenEntity();
        e.setId(id);
        e.setUserId(userId);
        e.setToken(token);
        e.setExpiresAt(expiresAt);
        e.setUsed(used);
        e.setEventId(eventId);
        e.setNotified(notified);
        return e;
    }
}
