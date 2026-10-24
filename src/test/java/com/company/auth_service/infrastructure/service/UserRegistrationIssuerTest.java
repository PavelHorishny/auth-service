package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.Roles;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.UserRegistrationIssuer;
import com.company.auth_service.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void register_completesRegistrationAndPersistsUser() {
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
    void markNotified_flipsFlagAndPersists() {
        User user = new User();
        user.completeRegistration("hashed-password", "event-1");

        userRegistrationIssuer.markNotified(user);

        assertThat(user.isNotified()).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(user);
    }
}
