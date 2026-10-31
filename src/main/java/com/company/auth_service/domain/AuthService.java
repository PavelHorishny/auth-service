package com.company.auth_service.domain;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailEventPublisher mailEventPublisher;
    private final UserRegistrationIssuer userRegistrationIssuer;

    public User register(User newUser, String rawPassword) {
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new EmailAlreadyRegisteredException(newUser.getEmail());
        }

        User user;
        try {
            user = userRegistrationIssuer.register(newUser, passwordEncoder.encode(rawPassword));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException(newUser.getEmail());
        }

        try {
            mailEventPublisher.publish(UserRegistrationMailEventFactory.from(user));
        } catch (Exception e) {
            log.warn("Failed to publish new-user event for {}, will retry via scheduler", user.getId(), e);
            return user;
        }
        try {
            userRegistrationIssuer.markNotified(user);
        } catch (Exception e) {
            log.warn("Published new-user event for {} but failed to mark it notified; " +
                    "scheduler will resend (harmless - consumer dedupes by eventId)", user.getId(), e);
        }
        return user;
    }
}
