package com.company.auth_service.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, Instant now);
    //List<PasswordResetToken> findAllByNotifiedFalse();
    List<PendingPasswordResetNotification> lockPendingNotifications(int limit);
    int markNotified(Collection<UUID> ids);
    int markExpiredNotified(Instant now);
    PasswordResetToken save(PasswordResetToken passwordResetToken);
}
