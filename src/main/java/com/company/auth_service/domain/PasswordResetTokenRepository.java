package com.company.auth_service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, Instant now);
    List<PasswordResetToken> findAllByNotifiedFalse();
    PasswordResetToken save(PasswordResetToken passwordResetToken);
}
