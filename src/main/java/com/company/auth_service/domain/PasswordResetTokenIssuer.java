package com.company.auth_service.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenIssuer {

    @Value("${auth.password-reset.token-ttl-minutes}")
    private long tokenTtlMinutes;

    private final PasswordResetTokenRepository tokenRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PasswordResetToken issue(User user) {
        Instant expiresAt = Instant.now().plus(tokenTtlMinutes, ChronoUnit.MINUTES);
        PasswordResetToken resetToken = PasswordResetToken.issue(
                user, UUID.randomUUID().toString(), expiresAt, UUID.randomUUID().toString());
        return tokenRepository.save(resetToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNotified(PasswordResetToken resetToken) {
        resetToken.markNotified();
        tokenRepository.save(resetToken);
    }
}
