package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.*;
import com.company.auth_service.infrastructure.repository.PasswordResetTokenRepository;
import com.company.auth_service.infrastructure.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    @Value("${auth.password-reset.token-ttl-minutes}")
    private long tokenTtlMinutes;

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
        resetToken.setExpiresAt(Instant.now().plus(tokenTtlMinutes, ChronoUnit.MINUTES));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mailEventPublisher.publish(MailEventMessage.builder()
                        .eventId(UUID.randomUUID().toString())
                        .recipient(user.getEmail())
                        .templateKey(TemplateKeys.PASSWORD_RESET)
                        .locale(Locales.EN)
                        .variables(Map.of("resetToken", resetToken.getToken()))
                        .build());
            }
        });
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(token, Instant.now())
                .orElseThrow(InvalidResetTokenException::new);
        /*PasswordResetToken resetToken = tokenRepository.findValidToken(token, Instant.now())
                .orElseThrow(InvalidResetTokenException::new);*/


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