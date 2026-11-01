package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.mailevent.MailEventMessage;
import com.company.auth_service.domain.mailevent.MailEventPublisher;
import com.company.auth_service.domain.mailevent.TemplateKeys;
import com.company.auth_service.domain.services.UserNotificationRetryService;
import com.company.auth_service.domain.user.PendingUserNotification;
import com.company.auth_service.domain.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationRetryServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MailEventPublisher mailEventPublisher;

    private UserNotificationRetryService service;

    private final UUID id1 = UUID.randomUUID();
    private final UUID id2 = UUID.randomUUID();
    private final UUID id3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserNotificationRetryService(userRepository, mailEventPublisher);
    }

    @Test
    void retryBatch_publishesEveryClaimedUserAndMarksThemNotifiedInOneStatement() {
        when(userRepository.lockPendingNotifications(10)).thenReturn(List.of(
                new PendingUserNotification(id1, "event-1", "a@example.com"),
                new PendingUserNotification(id2, "event-2", "b@example.com")));

        int delivered = service.retryBatch(10);

        assertThat(delivered).isEqualTo(2);
        ArgumentCaptor<MailEventMessage> captor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(MailEventMessage::eventId)
                .containsExactly("event-1", "event-2");
        assertThat(captor.getAllValues()).extracting(MailEventMessage::templateKey)
                .containsOnly(TemplateKeys.NEW_USER);
        verify(userRepository).markNotified(List.of(id1, id2));
    }

    @Test
    void retryBatch_stopsAtTheFirstFailureAndMarksOnlyWhatWasDelivered() {
        when(userRepository.lockPendingNotifications(10)).thenReturn(List.of(
                new PendingUserNotification(id1, "event-1", "a@example.com"),
                new PendingUserNotification(id2, "event-2", "b@example.com"),
                new PendingUserNotification(id3, "event-3", "c@example.com")));
        doNothing().doThrow(new RuntimeException("kafka down")).when(mailEventPublisher).publish(any());

        int delivered = service.retryBatch(10);

        assertThat(delivered).isEqualTo(1);
        verify(mailEventPublisher, times(2)).publish(any());      // третий даже не пробуем
        verify(userRepository).markNotified(List.of(id1));
    }

    @Test
    void retryBatch_doesNothingWhenNoUserIsPending() {
        when(userRepository.lockPendingNotifications(10)).thenReturn(List.of());

        assertThat(service.retryBatch(10)).isZero();

        verifyNoInteractions(mailEventPublisher);
    }
}
