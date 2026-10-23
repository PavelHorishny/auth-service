package com.company.auth_service.infrastructure.scheduler;

import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.PasswordResetTokenRepository;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.PasswordResetTokenIssuer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetNotificationRetrySchedulerTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private MailEventPublisher mailEventPublisher;
    @Mock
    private PasswordResetTokenIssuer tokenIssuer;

    private PasswordResetNotificationRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PasswordResetNotificationRetryScheduler(tokenRepository, mailEventPublisher, tokenIssuer);
    }

    private PasswordResetToken tokenFor(String email, Instant expiresAt) {
        User user = new User();
        user.setEmail(email);
        return PasswordResetToken.issue(user, "tok-" + email, expiresAt, "event-" + email);
    }

    @Test
    void retryUnnotified_publishesAndMarksNotifiedForPendingValidTokens() {
        PasswordResetToken token = tokenFor("user@example.com", Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findAllByNotifiedFalse()).thenReturn(List.of(token));

        scheduler.retryUnnotified();

        ArgumentCaptor<MailEventMessage> eventCaptor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo("event-user@example.com");
        verify(tokenIssuer).markNotified(token);
    }

    @Test
    void retryUnnotified_givesUpOnExpiredTokensWithoutPublishing() {
        PasswordResetToken expired = tokenFor("expired@example.com", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findAllByNotifiedFalse()).thenReturn(List.of(expired));

        scheduler.retryUnnotified();

        verify(mailEventPublisher, never()).publish(any());
        verify(tokenIssuer).markNotified(expired);
    }

    @Test
    void retryUnnotified_leavesTokenUnnotifiedWhenPublishFailsAgain() {
        PasswordResetToken token = tokenFor("user@example.com", Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findAllByNotifiedFalse()).thenReturn(List.of(token));
        doThrow(new RuntimeException("kafka still down")).when(mailEventPublisher).publish(any());

        scheduler.retryUnnotified();

        verify(tokenIssuer, never()).markNotified(any());
    }
}
