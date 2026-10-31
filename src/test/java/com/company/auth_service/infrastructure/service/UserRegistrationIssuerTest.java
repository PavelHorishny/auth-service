package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.Roles;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.UserRegistrationIssuer;
import com.company.auth_service.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationIssuerTest {

    @Mock
    private UserRepository userRepository;

    private UserRegistrationIssuer userRegistrationIssuer;

    @BeforeEach
    void setUp() {
        userRegistrationIssuer = new UserRegistrationIssuer(userRepository);
    }

    @Test
    void register_completesRegistrationAndPersistsUser() {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User user = new User();
        user.setEmail("user@example.com");

        User saved = userRegistrationIssuer.register(user, "hashed-password");

        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getRole()).isEqualTo(Roles.USER);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isNotified()).isFalse();
        assertThat(saved.getEventId()).isNotBlank();
        verify(userRepository).save(user);
    }

    @Test
    void markNotified_flipsFlagWithSingleUpdateInsteadOfRewritingTheRow() {
        User user = new User();
        user.setId(UUID.randomUUID());                       // List.of(null) бросил бы NPE
        user.completeRegistration("hashed-password", "event-1");

        userRegistrationIssuer.markNotified(user);

        assertThat(user.isNotified()).isTrue();
        verify(userRepository).markNotified(List.of(user.getId()));
        verify(userRepository, never()).save(any());
    }
}