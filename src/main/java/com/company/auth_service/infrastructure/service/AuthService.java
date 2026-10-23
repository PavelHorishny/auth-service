package com.company.auth_service.infrastructure.service;


import com.company.auth_service.domain.*;
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
    private final UserRegistrationIssuer userRegistrationIssuer;

    public User register(User newUser, String rawPassword) {

        User user = userRegistrationIssuer.register(newUser, passwordEncoder.encode(rawPassword));
        try{
            mailEventPublisher.publish(UserRegistrationMailEventFactory.from(user));

        } catch (Exception e) {
            log.warn("Failed to publish new-user event for {}, will retry via scheduler", user.getId(), e);
            return user;
        }
        try {
            userRegistrationIssuer.markNotified(user);
        } catch (Exception e) {
            log.warn("Published new-user event for {} but failed to mark it notified; " +
                    "scheduler will resend (harmless - consumer dedupes by eventID", user.getId(), e);
        }

        return user;
    }
    //TODO rework method to claim user not request and move to domain
}
