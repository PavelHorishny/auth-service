package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.User;
import com.company.auth_service.domain.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationIssuer {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User register(User user, String passwordHash) {
        user.completeRegistration(passwordHash, UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNotified(User user) {
        user.markNotified();
        userRepository.save(user);
    }
}
