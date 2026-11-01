package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.TestcontainersConfiguration;
import com.company.auth_service.fixtures.PasswordResetTokenFixture;
import com.company.auth_service.fixtures.UserFixture;
import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class PasswordResetTokenRepositoryIT {

    @Autowired
    private TokenJpaDao tokenRepository;

    @Autowired
    private UserJpaDao userRepository;

    // ---------- поиск валидного токена: derived-метод и @Query должны совпадать ----------

    @Test
    void agreeOnValidToken() {
        PasswordResetTokenEntity token = saveToken(PasswordResetTokenFixture.getInstance()
                .withToken("valid-token"));

        assertBothMethodsReturn("valid-token", token.getId());
    }

    @Test
    void agreeOnUsedToken() {
        saveToken(PasswordResetTokenFixture.getInstance().withToken("used-token").withUsed(true));

        assertBothMethodsReturn("used-token", null);
    }

    @Test
    void agreeOnExpiredToken() {
        saveToken(PasswordResetTokenFixture.getInstance()
                .withToken("expired-token")
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)));

        assertBothMethodsReturn("expired-token", null);
    }

    @Test
    void agreeOnUnknownToken() {
        assertBothMethodsReturn("no-such-token", null);
    }

    // ---------- шедулер: claim и bulk update ----------

    @Test
    void lockPendingNotifications_returnsOnlyUnnotifiedTokensAsProjection() {
        PasswordResetTokenEntity pending = saveToken(PasswordResetTokenFixture.getInstance()
                .withToken("pending-token"));
        saveToken(PasswordResetTokenFixture.getInstance().withNotified(true));

        List<TokenJpaDao.PendingNotificationRow> rows = tokenRepository.lockPendingNotifications(10);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(pending.getId());
        assertThat(rows.get(0).getToken()).isEqualTo("pending-token");
        assertThat(rows.get(0).getEventId()).isEqualTo(pending.getEventId());
        assertThat(rows.get(0).getUserId()).isEqualTo(pending.getUserId());
    }

    @Test
    void lockPendingNotifications_respectsTheLimit() {
        for (int i = 0; i < 3; i++) {
            saveToken(PasswordResetTokenFixture.getInstance());
        }
        tokenRepository.flush();

        assertThat(tokenRepository.lockPendingNotifications(2)).hasSize(2);
    }

    @Test
    void markNotified_flipsOnlyTheGivenTokens() {
        PasswordResetTokenEntity first = saveToken(PasswordResetTokenFixture.getInstance());
        PasswordResetTokenEntity second = saveToken(PasswordResetTokenFixture.getInstance());

        int updated = tokenRepository.markNotified(List.of(first.getId()));

        assertThat(updated).isEqualTo(1);
        assertThat(tokenRepository.findById(first.getId()).orElseThrow().isNotified()).isTrue();
        assertThat(tokenRepository.findById(second.getId()).orElseThrow().isNotified()).isFalse();
    }

    @Test
    void markExpiredNotified_closesOnlyExpiredPendingTokens() {
        PasswordResetTokenEntity expired = saveToken(PasswordResetTokenFixture.getInstance()
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)));
        PasswordResetTokenEntity valid = saveToken(PasswordResetTokenFixture.getInstance());

        int closed = tokenRepository.markExpiredNotified(Instant.now());

        assertThat(closed).isEqualTo(1);
        assertThat(tokenRepository.findById(expired.getId()).orElseThrow().isNotified()).isTrue();
        assertThat(tokenRepository.findById(valid.getId()).orElseThrow().isNotified()).isFalse();
    }

    // ---------- погашение токена ----------

    @Test
    void markUsed_succeedsOnceThenReportsZeroRows() {
        PasswordResetTokenEntity token = saveToken(PasswordResetTokenFixture.getInstance());

        assertThat(tokenRepository.markUsed(token.getId())).isEqualTo(1);
        assertThat(tokenRepository.markUsed(token.getId())).isEqualTo(0);
        assertThat(tokenRepository.findById(token.getId()).orElseThrow().isUsed()).isTrue();
    }

    // ---------- ограничения схемы (миграции 002 и 006) ----------

    @Test
    void tokenOfUnknownUser_isRejectedByForeignKey() {
        PasswordResetTokenEntity orphan = PasswordResetTokenFixture.getInstance()
                .withUserId(UUID.randomUUID())          // такого пользователя нет
                .toEntity();

        assertThatThrownBy(() -> tokenRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tokenLongerThan36_isRejectedByDatabase() {
        PasswordResetTokenFixture tooLong = PasswordResetTokenFixture.getInstance().withToken("t".repeat(37));

        assertThatThrownBy(() -> saveToken(tooLong))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- helpers ----------

    private UserEntity saveUser() {
        return userRepository.save(UserFixture.getInstance().toEntity());
    }

    /** Сохраняет токен для нового пользователя (FK требует существующего user_id). */
    private PasswordResetTokenEntity saveToken(PasswordResetTokenFixture fixture) {
        UserEntity user = saveUser();
        return tokenRepository.saveAndFlush(fixture.withUserId(user.getId()).toEntity());
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
}