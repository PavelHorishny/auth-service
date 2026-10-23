package com.company.auth_service.infrastructure.scheduler;

import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.UserRepository;
import com.company.auth_service.infrastructure.service.UserRegistrationIssuer;
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
class UserRegistrationNotificationRetrySchedulerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MailEventPublisher mailEventPublisher;
    @Mock
    private UserRegistrationIssuer userRegistrationIssuer;

    private UserRegistrationNotificationRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UserRegistrationNotificationRetryScheduler(userRepository, mailEventPublisher, userRegistrationIssuer);
    }

    private User userFor(String email) {
        User user = new User();
        user.setEmail(email);
        user.completeRegistration("hashed", "event-" + email);
        return user;
    }

    @Test
    void retryUnnotified_publishesAndMarksNotifiedForPendingUsers() {
        User user = userFor("user@example.com");
        when(userRepository.findAllByNotifiedFalse()).thenReturn(List.of(user));

        scheduler.retryUnnotified();

        ArgumentCaptor<MailEventMessage> eventCaptor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo("event-user@example.com");
        verify(userRegistrationIssuer).markNotified(user);
    }

    @Test
    void retryUnnotified_leavesUserUnnotifiedWhenPublishFailsAgain() {
        User user = userFor("user@example.com");
        when(userRepository.findAllByNotifiedFalse()).thenReturn(List.of(user));
        doThrow(new RuntimeException("kafka still down")).when(mailEventPublisher).publish(any());

        scheduler.retryUnnotified();

        verify(userRegistrationIssuer, never()).markNotified(any());
    }
}
