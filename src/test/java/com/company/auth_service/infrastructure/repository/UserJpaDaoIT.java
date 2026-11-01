package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.TestcontainersConfiguration;
import com.company.auth_service.domain.Roles;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import java.time.Instant;
import java.util.List;
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
class UserJpaDaoIT {

    @Autowired
    private UserJpaDao userDao;

    @Test
    void lockPendingNotifications_returnsOnlyUnnotifiedUsersAsProjection() {
        UserEntity pending = persistUser("pending@example.com", "event-1", false);
        persistUser("done@example.com", "event-2", true);
        userDao.flush();

        List<UserJpaDao.PendingNotificationRow> rows = userDao.lockPendingNotifications(10);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(pending.getId());
        assertThat(rows.get(0).getEventId()).isEqualTo("event-1");
        assertThat(rows.get(0).getEmail()).isEqualTo("pending@example.com");
    }

    @Test
    void markNotified_flipsOnlyGivenUsers() {
        UserEntity first = persistUser("first@example.com", "event-1", false);
        UserEntity second = persistUser("second@example.com", "event-2", false);

        int updated = userDao.markNotified(List.of(first.getId()), Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(userDao.findById(first.getId()).orElseThrow().isNotified()).isTrue();
        assertThat(userDao.findById(second.getId()).orElseThrow().isNotified()).isFalse();
    }

    @Test
    void findEmailsByIds_returnsKnownUsersOnly() {
        UserEntity user = persistUser("known@example.com", "event-1", false);

        List<UserJpaDao.EmailRow> rows = userDao.findEmailsByIds(List.of(user.getId(), UUID.randomUUID()));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEmail()).isEqualTo("known@example.com");
    }

    @Test
    void updatePasswordHash_changesOnlyThatUser() {
        UserEntity target = persistUser("target@example.com", "event-1", false);
        UserEntity other = persistUser("other@example.com", "event-2", false);

        int updated = userDao.updatePasswordHash(target.getId(), "new-hash", Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(userDao.findById(target.getId()).orElseThrow().getPasswordHash()).isEqualTo("new-hash");
        assertThat(userDao.findById(other.getId()).orElseThrow().getPasswordHash()).isEqualTo("hash");
        assertThat(userDao.updatePasswordHash(UUID.randomUUID(), "x", Instant.now())).isEqualTo(0);
    }

    private UserEntity persistUser(String email, String eventId, boolean notified) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(Roles.USER);
        user.setEnabled(true);
        user.setEventId(eventId);
        user.setNotified(notified);
        return userDao.save(user);
    }
}
