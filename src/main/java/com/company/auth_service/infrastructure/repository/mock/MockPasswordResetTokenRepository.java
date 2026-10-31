package com.company.auth_service.infrastructure.repository.mock;

import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.PasswordResetTokenRepository;
import com.company.auth_service.domain.PendingPasswordResetNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
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
    public List<PendingPasswordResetNotification> lockPendingNotifications(int limit) {
        return tokens.values().stream()
                .filter(t->!t.isNotified())
                .limit(limit)
                .map(t->new PendingPasswordResetNotification(
                        t.getId(),t.getEventId(),t.getToken(),t.getUser().getId()
                )).toList();
    }

    @Override
    public int markNotified(Collection<UUID> ids) {
        int updated = 0;
        for(UUID id : ids){
            PasswordResetToken token = tokens.get(id);
            if(token != null && !token.isNotified()){
                token.markNotified();
                updated++;
            }
        }
        return updated;
    }

    @Override
    public int markExpiredNotified(Instant now) {
        int updated = 0;
        for( PasswordResetToken token : tokens.values()){
            if(!token.isNotified() && token.getExpiresAt().isBefore(now)){
                token.markNotified();
                updated++;
            }
        }
        return updated;
    }


    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        UUID id = passwordResetToken.getId() != null ? passwordResetToken.getId() : UUID.randomUUID();
        passwordResetToken.setId(id);
        tokens.put(id, passwordResetToken);
        return passwordResetToken;
    }

}