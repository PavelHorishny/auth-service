package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.InvalidResetTokenException;
import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.TemplateKeys;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.PasswordResetTokenRepository;
import com.company.auth_service.domain.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private MailEventPublisher mailEventPublisher;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetTokenIssuer tokenIssuer;

    private PasswordResetService passwordResetService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository, tokenRepository, mailEventPublisher, passwordEncoder, tokenIssuer);
    }

    @Test
    void requestReset_doesNothingWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("missing@example.com");

        verifyNoInteractions(tokenIssuer, mailEventPublisher);
    }

    @Test
    void requestReset_issuesTokenPublishesEventAndMarksNotified() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken issuedToken = PasswordResetToken.issue(
                user, "token-value", Instant.now().plus(30, ChronoUnit.MINUTES), "event-123");
        when(tokenIssuer.issue(user)).thenReturn(issuedToken);

        passwordResetService.requestReset("user@example.com");

        ArgumentCaptor<MailEventMessage> eventCaptor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo("event-123");
        assertThat(eventCaptor.getValue().templateKey()).isEqualTo(TemplateKeys.PASSWORD_RESET);
        assertThat(eventCaptor.getValue().recipient()).isEqualTo("user@example.com");

        verify(tokenIssuer).markNotified(issuedToken);
    }

    @Test
    void requestReset_leavesTokenUnnotifiedWhenPublishFails() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken issuedToken = PasswordResetToken.issue(
                user, "token-value", Instant.now().plus(30, ChronoUnit.MINUTES), "event-123");
        when(tokenIssuer.issue(user)).thenReturn(issuedToken);
        org.mockito.Mockito.doThrow(new RuntimeException("kafka down"))
                .when(mailEventPublisher).publish(any());

        passwordResetService.requestReset("user@example.com");

        verify(tokenIssuer, never()).markNotified(any());
    }

    @Test
    void confirmReset_updatesPasswordAndMarksTokenUsed() {
        User user = new User();
        user.setPasswordHash("old-hash");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenAndUsedFalseAndExpiresAtAfter(eq("tok"), any()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword")).thenReturn("new-hash");

        passwordResetService.confirmReset("tok", "newPassword");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void confirmReset_throwsWhenTokenInvalid() {
        when(tokenRepository.findByTokenAndUsedFalseAndExpiresAtAfter(eq("bad"), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.confirmReset("bad", "newPassword"))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }
}
