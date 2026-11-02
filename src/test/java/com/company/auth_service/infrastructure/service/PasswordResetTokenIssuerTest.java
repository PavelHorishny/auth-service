package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.token.PasswordResetToken;
import com.company.auth_service.domain.token.PasswordResetTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.company.auth_service.domain.token.PasswordResetTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    }

    @Test
    void issue_buildsUnnotifiedUnusedTokenWithConfiguredTtlAndPersistsIt() {

        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID userId = UUID.randomUUID();


        Instant before = Instant.now();
        PasswordResetToken token = tokenIssuer.issue(userId);
        Instant after = Instant.now();

        assertThat(token.getUserId()).isEqualTo(userId);
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
    void markNotified_flipsFlagWithSingleUpdateInsteadOfRewritingTheRow() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), "tok", Instant.now(), "event-1");
        token.setId(UUID.randomUUID());

        tokenIssuer.markNotified(token);

        assertThat(token.isNotified()).isTrue();
        verify(tokenRepository).markNotified(List.of(token.getId()));
        verify(tokenRepository, never()).save(any());
    }
}
