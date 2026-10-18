package com.company.auth_service.infrastructure.service;


import com.company.auth_service.domain.*;
import com.company.auth_service.infrastructure.web.RegisterRequest;
import com.company.auth_service.infrastructure.web.mapper.UserWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository; //to work with DB
    private final PasswordEncoder passwordEncoder; //to encode passwords
    private final MailEventPublisher mailEventPublisher; //to send events by Kafka
    private final UserWebMapper userMapper;
    private final UserRegistrationIssuer userRegistrationIssuer;

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User mapped = userMapper.toUser(request);
        User user = userRegistrationIssuer.register(mapped, passwordEncoder.encode(request.password()));

        try {
            mailEventPublisher.publish(UserRegistrationMailEventFactory.from(user));
            userRegistrationIssuer.markNotified(user);
        } catch (Exception e) {
            log.warn("Failed to publish new-user event for {}, will retry via scheduler", user.getId(), e);
        }

        return user;
    }
}
