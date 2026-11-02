package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.mailevent.MailEventMessage;
import com.company.auth_service.domain.mailevent.MailEventPublisher;
import com.company.auth_service.domain.mailevent.TemplateKeys;
import com.company.auth_service.domain.services.PasswordResetNotificationRetryService;
import com.company.auth_service.domain.token.PasswordResetTokenRepository;
import com.company.auth_service.domain.token.PendingPasswordResetNotification;
import com.company.auth_service.domain.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetNotificationRetryServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MailEventPublisher mailEventPublisher;

    private PasswordResetNotificationRetryService service;

    private final UUID tokenId1 = UUID.randomUUID();
    private final UUID tokenId2 = UUID.randomUUID();
    private final UUID userId1 = UUID.randomUUID();
    private final UUID userId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PasswordResetNotificationRetryService(tokenRepository, userRepository, mailEventPublisher);
    }

    @Test
    void giveUpOnExpired_closesAllExpiredTokensWithOneStatement() {
        when(tokenRepository.markExpiredNotified(any())).thenReturn(3);

        assertThat(service.giveUpOnExpired()).isEqualTo(3);
    }

    @Test
    void retryBatch_resolvesEmailsForTheWholeBatchPublishesAndMarksTokensNotified() {
        when(tokenRepository.lockPendingNotifications(10)).thenReturn(List.of(
                new PendingPasswordResetNotification(tokenId1, "event-1", "token-1", userId1),
                new PendingPasswordResetNotification(tokenId2, "event-2", "token-2", userId2)));
        when(userRepository.findEmailsByIds(List.of(userId1, userId2)))
                .thenReturn(Map.of(userId1, "a@example.com", userId2, "b@example.com"));

        int processed = service.retryBatch(10);

        assertThat(processed).isEqualTo(2);
        ArgumentCaptor<MailEventMessage> captor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(MailEventMessage::recipient)
                .containsExactly("a@example.com", "b@example.com");
        assertThat(captor.getAllValues()).extracting(MailEventMessage::templateKey)
                .containsOnly(TemplateKeys.PASSWORD_RESET);
        assertThat(captor.getAllValues().get(0).variables()).containsEntry("resetToken", "token-1");
        verify(tokenRepository).markNotified(List.of(tokenId1, tokenId2));
    }

    @Test
    void retryBatch_dropsTokensWhoseUserNoLongerExistsWithoutPublishing() {
        when(tokenRepository.lockPendingNotifications(10)).thenReturn(List.of(
                new PendingPasswordResetNotification(tokenId1, "event-1", "token-1", userId1)));
        when(userRepository.findEmailsByIds(List.of(userId1))).thenReturn(Map.of());

        int processed = service.retryBatch(10);

        assertThat(processed).isEqualTo(1);
        verifyNoInteractions(mailEventPublisher);
        verify(tokenRepository).markNotified(List.of(tokenId1));
    }

    @Test
    void retryBatch_stopsAtTheFirstFailureAndLeavesTheRestPending() {
        when(tokenRepository.lockPendingNotifications(10)).thenReturn(List.of(
                new PendingPasswordResetNotification(tokenId1, "event-1", "token-1", userId1),
                new PendingPasswordResetNotification(tokenId2, "event-2", "token-2", userId2)));
        when(userRepository.findEmailsByIds(List.of(userId1, userId2)))
                .thenReturn(Map.of(userId1, "a@example.com", userId2, "b@example.com"));
        doThrow(new RuntimeException("kafka down")).when(mailEventPublisher).publish(any());

        int processed = service.retryBatch(10);

        assertThat(processed).isZero();
        verify(mailEventPublisher, times(1)).publish(any());
        verify(tokenRepository).markNotified(List.of());
    }

    @Test
    void retryBatch_doesNothingWhenNoTokenIsPending() {
        when(tokenRepository.lockPendingNotifications(10)).thenReturn(List.of());

        assertThat(service.retryBatch(10)).isZero();

        verifyNoInteractions(userRepository, mailEventPublisher);
        verify(tokenRepository, never()).markNotified(any());
    }
}