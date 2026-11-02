package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.TestcontainersConfiguration;
import com.company.auth_service.fixtures.UserFixture;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import java.time.Instant;
import java.util.List;
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
class UserJpaDaoIT {

    @Autowired
    private UserJpaDao userDao;

    @Test
    void lockPendingNotifications_returnsOnlyUnnotifiedUsersAsProjection() {
        UserEntity pending = userDao.save(UserFixture.getInstance()
                .withEmail("pending@example.com").toEntity());
        userDao.save(UserFixture.getInstance().withNotified(true).toEntity());
        userDao.flush();

        List<UserJpaDao.PendingNotificationRow> rows = userDao.lockPendingNotifications(10);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(pending.getId());
        assertThat(rows.get(0).getEventId()).isEqualTo(pending.getEventId());
        assertThat(rows.get(0).getEmail()).isEqualTo("pending@example.com");
    }

    @Test
    void lockPendingNotifications_respectsTheLimit() {
        for (int i = 0; i < 3; i++) {
            userDao.save(UserFixture.getInstance().toEntity());
        }
        userDao.flush();

        assertThat(userDao.lockPendingNotifications(2)).hasSize(2);
    }

    @Test
    void markNotified_flipsOnlyGivenUsers() {
        UserEntity first = userDao.save(UserFixture.getInstance().toEntity());
        UserEntity second = userDao.save(UserFixture.getInstance().toEntity());

        int updated = userDao.markNotified(List.of(first.getId()), Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(userDao.findById(first.getId()).orElseThrow().isNotified()).isTrue();
        assertThat(userDao.findById(second.getId()).orElseThrow().isNotified()).isFalse();
    }

    @Test
    void findEmailsByIds_returnsKnownUsersOnly() {
        UserEntity user = userDao.save(UserFixture.getInstance()
                .withEmail("known@example.com").toEntity());

        List<UserJpaDao.EmailRow> rows = userDao.findEmailsByIds(List.of(user.getId(), UUID.randomUUID()));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEmail()).isEqualTo("known@example.com");
    }

    @Test
    void updatePasswordHash_changesOnlyThatUser() {
        UserEntity target = userDao.save(UserFixture.getInstance().withPasswordHash("old-hash").toEntity());
        UserEntity other = userDao.save(UserFixture.getInstance().withPasswordHash("old-hash").toEntity());

        int updated = userDao.updatePasswordHash(target.getId(), "new-hash", Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(userDao.findById(target.getId()).orElseThrow().getPasswordHash()).isEqualTo("new-hash");
        assertThat(userDao.findById(other.getId()).orElseThrow().getPasswordHash()).isEqualTo("old-hash");
        assertThat(userDao.updatePasswordHash(UUID.randomUUID(), "x", Instant.now())).isEqualTo(0);
    }


    @Test
    void duplicateEmail_isRejectedByUniqueConstraint() {
        userDao.saveAndFlush(UserFixture.getInstance().withEmail("dup@example.com").toEntity());

        assertThatThrownBy(() -> userDao.saveAndFlush(
                UserFixture.getInstance().withEmail("dup@example.com").toEntity()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailLongerThan254_isRejectedByDatabase() {
        String tooLong = "a".repeat(255);

        assertThatThrownBy(() -> userDao.saveAndFlush(UserFixture.getInstance().withEmail(tooLong).toEntity()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailOfExactly254_isAccepted() {
        String maxLength = "a".repeat(254);

        UserEntity saved = userDao.saveAndFlush(UserFixture.getInstance().withEmail(maxLength).toEntity());

        assertThat(userDao.findById(saved.getId()).orElseThrow().getEmail()).hasSize(254);
    }
}