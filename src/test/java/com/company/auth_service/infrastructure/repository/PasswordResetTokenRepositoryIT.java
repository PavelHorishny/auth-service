package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.TestcontainersConfiguration;
import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.domain.Roles;
import com.company.auth_service.domain.User;
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

/**
 * findByTokenAndUsedFalseAndExpiresAtAfter (derived) and findValidToken (@Query) express the
 * same WHERE clause two different ways - this asserts they actually agree on every row shape.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class PasswordResetTokenRepositoryIT {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void agreeOnValidToken() {
        User user = persistUser("valid@example.com");
        PasswordResetToken token = persistToken(user, "valid-token", false, Instant.now().plus(10, ChronoUnit.MINUTES));

        assertBothMethodsReturn("valid-token", token.getId());
    }

    @Test
    void agreeOnUsedToken() {
        User user = persistUser("used@example.com");
        persistToken(user, "used-token", true, Instant.now().plus(10, ChronoUnit.MINUTES));

        assertBothMethodsReturn("used-token", null);
    }

    @Test
    void agreeOnExpiredToken() {
        User user = persistUser("expired@example.com");
        persistToken(user, "expired-token", false, Instant.now().minus(1, ChronoUnit.MINUTES));

        assertBothMethodsReturn("expired-token", null);
    }

    @Test
    void agreeOnUnknownToken() {
        assertBothMethodsReturn("no-such-token", null);
    }

    private void assertBothMethodsReturn(String token, UUID expectedTokenId) {
        Instant now = Instant.now();
        Optional<PasswordResetToken> derived = tokenRepository.findByTokenAndUsedFalseAndExpiresAtAfter(token, now);
        Optional<PasswordResetToken> viaQuery = tokenRepository.findValidToken(token, now);

        assertThat(derived.isPresent())
                .as("derived vs @Query presence for token '%s'", token)
                .isEqualTo(viaQuery.isPresent());

        if (expectedTokenId != null) {
            assertThat(derived).get().extracting(PasswordResetToken::getId).isEqualTo(expectedTokenId);
            assertThat(viaQuery).get().extracting(PasswordResetToken::getId).isEqualTo(expectedTokenId);
        } else {
            assertThat(derived).isEmpty();
            assertThat(viaQuery).isEmpty();
        }
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(Roles.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private PasswordResetToken persistToken(User user, String value, boolean used, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(value);
        token.setUsed(used);
        token.setExpiresAt(expiresAt);
        return tokenRepository.save(token);
    }
}
