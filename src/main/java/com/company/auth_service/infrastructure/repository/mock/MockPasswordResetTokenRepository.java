package com.company.auth_service.infrastructure.repository.mock;

import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.PasswordResetTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(prefix = "auth", name = "repository-mode", havingValue = "mock", matchIfMissing = true)
public class MockPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final ConcurrentHashMap<UUID, PasswordResetToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, Instant now) {
        return tokens.values().stream()
                .filter(t -> t.getToken().equals(token))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(now))
                .findFirst();
    }

    @Override
    public List<PasswordResetToken> findAllByNotifiedFalse() {
        return tokens.values().stream()
                .filter(t -> !t.isNotified())
                .toList();
    }

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        UUID id = passwordResetToken.getId() != null ? passwordResetToken.getId() : UUID.randomUUID();
        passwordResetToken.setId(id);
        tokens.put(id, passwordResetToken);
        return passwordResetToken;
    }

}