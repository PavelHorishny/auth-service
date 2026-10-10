package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.EmailAlreadyRegisteredException;
import com.company.auth_service.domain.Locales;
import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.Roles;
import com.company.auth_service.domain.TemplateKeys;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.web.RegisterRequest;
import com.company.auth_service.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, mailEventPublisher);
        // register() relies on an active transaction to defer the Kafka publish; a plain
        // unit test has no real transaction, so we simulate one manually.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void register_savesUserAndPublishesEventOnlyAfterCommit() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");

        User savedUser = authService.register(request);

        verify(userRepository).save(savedUser);
        verifyNoInteractions(mailEventPublisher);

        TransactionSynchronizationUtils.triggerAfterCommit();

        ArgumentCaptor<MailEventMessage> eventCaptor = ArgumentCaptor.forClass(MailEventMessage.class);
        verify(mailEventPublisher).publish(eventCaptor.capture());
        MailEventMessage event = eventCaptor.getValue();
        assertThat(event.recipient()).isEqualTo(request.email());
        assertThat(event.templateKey()).isEqualTo(TemplateKeys.NEW_USER);
        assertThat(event.locale()).isEqualTo(Locales.EN);
        assertThat(savedUser.getRole()).isEqualTo(Roles.USER);
    }

    @Test
    void register_doesNotPublishEventWhenTransactionRollsBack() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");

        authService.register(request);

        // simulate rollback: completion fires without a commit ever happening
        TransactionSynchronizationUtils.triggerAfterCompletion(
                org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(mailEventPublisher);
    }

    @Test
    void register_throwsWhenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(mailEventPublisher);
    }
}
