package com.company.auth_service.domain.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.company.auth_service.domain.repo.PasswordResetTokenRepository;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Service
    @RequiredArgsConstructor
    public static class PasswordResetTokenIssuer {

        @Value("${auth.password-reset.token-ttl-minutes}")
        private long tokenTtlMinutes;

        private final PasswordResetTokenRepository tokenRepository;


        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public PasswordResetToken issue(UUID userId) {
            Instant expiresAt = Instant.now().plus(tokenTtlMinutes, ChronoUnit.MINUTES);
            PasswordResetToken resetToken = PasswordResetToken.issue(
                    userId, UUID.randomUUID().toString(), expiresAt, UUID.randomUUID().toString());
            return tokenRepository.save(resetToken);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void markNotified(PasswordResetToken resetToken) {
            resetToken.markNotified();
            tokenRepository.markNotified(List.of(resetToken.getId()));
        }
    }
}


