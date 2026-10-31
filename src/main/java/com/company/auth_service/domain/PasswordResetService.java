package com.company.auth_service.domain;

import java.time.Instant;

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

        PasswordResetToken resetToken = tokenIssuer.issue(user);

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

        User user = resetToken.getUser();
        //user.changePassword(passwordEncoder.encode(newPassword));
        resetToken.redeem(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        //resetToken.markUsed();
        tokenRepository.save(resetToken);
    }
}
