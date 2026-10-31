package com.company.auth_service.domain;

import java.util.List;
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
    public void markNotified(User user) {//is it suitable for issue of using only one field or must i made to get certain field as an argument
        user.markNotified();
        userRepository.markNotified(List.of(user.getId()));
    }
}
