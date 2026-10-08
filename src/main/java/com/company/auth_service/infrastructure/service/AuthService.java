package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.EmailAlreadyRegisteredException;
import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.User;
import com.company.auth_service.infrastructure.repository.UserRepository;
import com.company.auth_service.domain.web.RegisterRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        user.setRole("USER");
        user.setEnabled(true);
        userRepository.save(user);

        mailEventPublisher.publish(MailEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .recipient(user.getEmail())
                .templateKey("new_user")
                .locale("ru")
                .variables(Map.of("email", user.getEmail()))
                .build());

        return user;
    }
}