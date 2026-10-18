package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.*;
import com.company.auth_service.infrastructure.web.RegisterRequest;
import com.company.auth_service.infrastructure.web.mapper.UserWebMapperImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailEventPublisher mailEventPublisher;
    @Mock
    private UserRegistrationIssuer userRegistrationIssuer;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, mailEventPublisher, new UserWebMapperImpl(), userRegistrationIssuer);
    }

    @Test
    void register_issuesUserPublishesEventAndMarksNotified() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");

        User issuedUser = new User();
        issuedUser.setEmail(request.email());
        issuedUser.completeRegistration("hashed", "event-123");
        when(userRegistrationIssuer.register(any(), eq("hashed"))).thenReturn(issuedUser);

        User savedUser = authService.register(request);

        assertThat(savedUser).isSameAs(issuedUser);
        assertThat(savedUser.getRole()).isEqualTo(Roles.USER);

        ArgumentCaptor<MailEventMessage> eventCaptor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher).publish(eventCaptor.capture());
        MailEventMessage event = eventCaptor.getValue();
        assertThat(event.eventId()).isEqualTo("event-123");
        assertThat(event.recipient()).isEqualTo(request.email());
        assertThat(event.templateKey()).isEqualTo(TemplateKeys.NEW_USER);
        assertThat(event.locale()).isEqualTo(Locales.EN);

        verify(userRegistrationIssuer).markNotified(issuedUser);
    }

    @Test
    void register_leavesUserUnnotifiedWhenPublishFails() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");

        User issuedUser = new User();
        issuedUser.setEmail(request.email());
        issuedUser.completeRegistration("hashed", "event-123");
        when(userRegistrationIssuer.register(any(), eq("hashed"))).thenReturn(issuedUser);
        doThrow(new RuntimeException("kafka down")).when(mailEventPublisher).publish(any());

        authService.register(request);

        verify(userRegistrationIssuer, never()).markNotified(any());
    }

    @Test
    void register_throwsWhenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verifyNoInteractions(userRegistrationIssuer, mailEventPublisher);
    }
}
