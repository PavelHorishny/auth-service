package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.*;
import com.company.auth_service.infrastructure.repository.UserRepository;
import com.company.auth_service.infrastructure.web.RegisterRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository; //to work with DB
    private final PasswordEncoder passwordEncoder; //to encode passwords
    private final MailEventPublisher mailEventPublisher; //to send events by Kafka

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Roles.USER);
        user.setEnabled(true);
        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mailEventPublisher.publish(MailEventMessage.builder()
                        .eventId(UUID.randomUUID().toString())
                        .recipient(user.getEmail())
                        .templateKey(TemplateKeys.NEW_USER)
                        .locale(Locales.EN)
                        .variables(Map.of("email", user.getEmail()))
                        .build());
            }
        });

        return user;
    }
}