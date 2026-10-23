package com.company.auth_service.infrastructure.scheduler;

import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.PasswordResetTokenRepository;
import com.company.auth_service.domain.PasswordResetMailEventFactory;
import com.company.auth_service.domain.PasswordResetTokenIssuer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetNotificationRetryScheduler {

    private final PasswordResetTokenRepository tokenRepository;
    private final MailEventPublisher mailEventPublisher;
    private final PasswordResetTokenIssuer tokenIssuer;

    @Scheduled(fixedDelayString = "${auth.password-reset.retry-interval-ms:60000}")
    public void retryUnnotified() {
        for (PasswordResetToken resetToken : tokenRepository.findAllByNotifiedFalse()) {
            if (resetToken.getExpiresAt().isBefore(Instant.now())) {
                log.info("Password reset token {} expired before its notification could be sent, giving up",
                        resetToken.getId());
                tokenIssuer.markNotified(resetToken);
                continue;
            }

            try {
                mailEventPublisher.publish(PasswordResetMailEventFactory.from(resetToken));
                tokenIssuer.markNotified(resetToken);
            } catch (Exception e) {
                log.warn("Retry publish failed for password reset token {}, will retry again later",
                        resetToken.getId(), e);
            }
        }
    }
}
