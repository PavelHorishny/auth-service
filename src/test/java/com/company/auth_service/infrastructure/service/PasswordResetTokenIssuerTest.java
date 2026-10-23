package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.PasswordResetTokenIssuer;
import com.company.auth_service.domain.PasswordResetTokenRepository;
import com.company.auth_service.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenIssuerTest {

    private static final long TOKEN_TTL_MINUTES = 30;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    private PasswordResetTokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        tokenIssuer = new PasswordResetTokenIssuer(tokenRepository);
        ReflectionTestUtils.setField(tokenIssuer, "tokenTtlMinutes", TOKEN_TTL_MINUTES);
        when(tokenRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issue_buildsUnnotifiedUnusedTokenWithConfiguredTtlAndPersistsIt() {
        User user = new User();
        user.setEmail("user@example.com");

        Instant before = Instant.now();
        PasswordResetToken token = tokenIssuer.issue(user);
        Instant after = Instant.now();

        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isNotified()).isFalse();
        assertThat(token.getEventId()).isNotBlank();
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getExpiresAt())
                .isAfterOrEqualTo(before.plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES))
                .isBeforeOrEqualTo(after.plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));

        verify(tokenRepository).save(token);
    }

    @Test
    void markNotified_flipsFlagAndPersists() {
        User user = new User();
        PasswordResetToken token = PasswordResetToken.issue(user, "tok", Instant.now(), "event-1");

        tokenIssuer.markNotified(token);

        assertThat(token.isNotified()).isTrue();
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(token);
    }
}
