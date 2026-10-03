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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailEventPublisher mailEventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
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

    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidResetTokenException::new);

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

/*
    Serwis obejmuje cały proces odzyskiwania hasła. Jeśli użytkownika nie ma, po prostu kończy działanie, aby uniknąć
    ataku metodą słownikową (brute-force) polegającego na wyłudzaniu adresów e-mail. Jeśli użytkownik istnieje, tworzy
    token lub weryfikuje token do resetowania hasła i wysyła wiadomość przez Kafkę do serwisu mailingowego
*/
}