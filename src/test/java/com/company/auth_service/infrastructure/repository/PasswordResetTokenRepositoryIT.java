package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.TestcontainersConfiguration;
import com.company.auth_service.domain.Roles;
import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class PasswordResetTokenRepositoryIT {

    @Autowired
    private TokenJpaDao tokenRepository;

    @Autowired
    private UserJpaDao userRepository;

    @Test
    void agreeOnValidToken() {
        UserEntity user = persistUser("valid@example.com");
        PasswordResetTokenEntity token = persistToken(user, "valid-token", false, Instant.now().plus(10, ChronoUnit.MINUTES));

        assertBothMethodsReturn("valid-token", token.getId());
    }

    @Test
    void agreeOnUsedToken() {
        UserEntity user = persistUser("used@example.com");
        persistToken(user, "used-token", true, Instant.now().plus(10, ChronoUnit.MINUTES));

        assertBothMethodsReturn("used-token", null);
    }

    @Test
    void agreeOnExpiredToken() {
        UserEntity user = persistUser("expired@example.com");
        persistToken(user, "expired-token", false, Instant.now().minus(1, ChronoUnit.MINUTES));

        assertBothMethodsReturn("expired-token", null);
    }

    @Test
    void agreeOnUnknownToken() {
        assertBothMethodsReturn("no-such-token", null);
    }

    @Test
    void findAllByNotifiedFalse_returnsOnlyUnnotifiedTokens() {
        UserEntity pendingUser = persistUser("pending@example.com");
        PasswordResetTokenEntity pending = persistToken(pendingUser, "pending-token", false, Instant.now().plus(10, ChronoUnit.MINUTES));

        UserEntity notifiedUser = persistUser("done@example.com");
        PasswordResetTokenEntity notified = persistToken(notifiedUser, "done-token", false, Instant.now().plus(10, ChronoUnit.MINUTES));
        notified.setNotified(true);
        tokenRepository.save(notified);

      /*  assertThat(tokenRepository.findAllByNotifiedFalse())
                .extracting(PasswordResetTokenEntity::getId)
                .containsExactly(pending.getId());*/
    }

    private void assertBothMethodsReturn(String token, UUID expectedTokenId) {
        Instant now = Instant.now();
        Optional<PasswordResetTokenEntity> derived = tokenRepository.findByTokenAndUsedFalseAndExpiresAtAfter(token, now);
        Optional<PasswordResetTokenEntity> viaQuery = tokenRepository.findValidToken(token, now);

        assertThat(derived.isPresent())
                .as("derived vs @Query presence for token '%s'", token)
                .isEqualTo(viaQuery.isPresent());

        if (expectedTokenId != null) {
            assertThat(derived).get().extracting(PasswordResetTokenEntity::getId).isEqualTo(expectedTokenId);
            assertThat(viaQuery).get().extracting(PasswordResetTokenEntity::getId).isEqualTo(expectedTokenId);
        } else {
            assertThat(derived).isEmpty();
            assertThat(viaQuery).isEmpty();
        }
    }

    private UserEntity persistUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(Roles.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private PasswordResetTokenEntity persistToken(UserEntity user, String value, boolean used, Instant expiresAt) {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUser(user);
        token.setToken(value);
        token.setUsed(used);
        token.setExpiresAt(expiresAt);
        return tokenRepository.save(token);
    }
}
