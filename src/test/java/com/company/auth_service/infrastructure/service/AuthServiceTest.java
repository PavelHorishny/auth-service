package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailEventPublisher mailEventPublisher;
    @Mock
    private UserRegistrationIssuer userRegistrationIssuer;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(passwordEncoder, mailEventPublisher, userRegistrationIssuer);
    }

    @Test
    void register_issuesUserPublishesEventAndMarksNotified() {
        User mapped = new User();
        mapped.setEmail("new@example.com");
        org.mockito.Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed");

        User issuedUser = new User();
        issuedUser.setEmail("new@example.com");
        issuedUser.completeRegistration("hashed", "event-123");
        org.mockito.Mockito.when(userRegistrationIssuer.register(mapped, "hashed")).thenReturn(issuedUser);

        User savedUser = authService.register(mapped, "password123");

        assertThat(savedUser).isSameAs(issuedUser);
        assertThat(savedUser.getRole()).isEqualTo(Roles.USER);

        ArgumentCaptor<MailEventMessage> eventCaptor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher).publish(eventCaptor.capture());
        MailEventMessage event = eventCaptor.getValue();
        assertThat(event.eventId()).isEqualTo("event-123");
        assertThat(event.recipient()).isEqualTo("new@example.com");
        assertThat(event.templateKey()).isEqualTo(TemplateKeys.NEW_USER);
        assertThat(event.locale()).isEqualTo(Locales.EN);

        verify(userRegistrationIssuer).markNotified(issuedUser);
    }

    @Test
    void register_leavesUserUnnotifiedWhenPublishFails() {
        User mapped = new User();
        mapped.setEmail("new@example.com");
        org.mockito.Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed");

        User issuedUser = new User();
        issuedUser.setEmail("new@example.com");
        issuedUser.completeRegistration("hashed", "event-123");
        org.mockito.Mockito.when(userRegistrationIssuer.register(mapped, "hashed")).thenReturn(issuedUser);
        doThrow(new RuntimeException("kafka down")).when(mailEventPublisher).publish(any());

        authService.register(mapped, "password123");

        verify(userRegistrationIssuer, never()).markNotified(any());
    }
}
