package com.company.auth_service.domain.services;

import java.time.Instant;

import com.company.auth_service.domain.exceptions.InvalidResetTokenException;
import com.company.auth_service.domain.mailevent.MailEventPublisher;
import com.company.auth_service.domain.mailevent.PasswordResetMailEventFactory;
import com.company.auth_service.domain.token.PasswordResetTokenRepository;
import com.company.auth_service.domain.user.UserRepository;
import com.company.auth_service.domain.token.PasswordResetToken;
import com.company.auth_service.domain.token.PasswordResetTokenIssuer;
import com.company.auth_service.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailEventPublisher mailEventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenIssuer tokenIssuer;

    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        PasswordResetToken resetToken = tokenIssuer.issue(user.getId());

        try {
            mailEventPublisher.publish(PasswordResetMailEventFactory.from(resetToken, user.getEmail()));
        } catch (Exception e) {
            log.warn("Failed to publish password reset event for token {}, will retry via scheduler",
                    resetToken.getId(), e);
            return;
        }

        try {
            tokenIssuer.markNotified(resetToken);
        } catch (Exception e) {
            log.warn("Published password reset event for token {} but failed to mark it notified; " +
                    "scheduler will resend (harmless - consumer dedupes by eventId)", resetToken.getId(), e);
        }
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(token, Instant.now())
                .orElseThrow(InvalidResetTokenException::new);

        if (!tokenRepository.markUsed(resetToken.getId())){
            throw new InvalidResetTokenException();
        }
        userRepository.updatePasswordHash(resetToken.getUserId(),passwordEncoder.encode(newPassword));
    }
}
