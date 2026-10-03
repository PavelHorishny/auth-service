package com.company.auth_service.service;

import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.User;
import com.company.auth_service.repository.PasswordResetTokenRepository;
import com.company.auth_service.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailEventPublisher mailEventPublisher;

    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return; // не раскрываем, существует ли email
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        mailEventPublisher.publish(MailEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .recipient(user.getEmail())
                .templateKey("password_reset")
                .locale("ru")
                .variables(Map.of("resetToken", resetToken.getToken()))
                .build());
    }
}