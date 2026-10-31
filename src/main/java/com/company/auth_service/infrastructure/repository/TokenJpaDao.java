package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenJpaDao extends JpaRepository<PasswordResetTokenEntity, UUID> {
    interface PendingNotificationRow { UUID getId(); String getEventId(); String getToken(); UUID getUserId(); }

    Optional<PasswordResetTokenEntity> findByTokenAndUsedFalseAndExpiresAtAfter(String token, Instant now);
    @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.token = :token AND t.used = false AND t.expiresAt > :now")
    Optional<PasswordResetTokenEntity> findValidToken(@Param("token") String token, @Param("now") Instant now);
    //List<PasswordResetTokenEntity> findAllByNotifiedFalse();
    @Query(value = """
        SELECT id AS "id", event_id AS "eventId", token AS "token", user_id AS "userId"
        FROM password_reset_tokens WHERE notified = false
        ORDER BY created_at LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PendingNotificationRow> lockPendingNotifications(@Param("limit") int limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PasswordResetTokenEntity t SET t.notified = true WHERE t.id IN :ids AND t.notified = false")
    int markNotified(@Param("ids") Collection<UUID> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PasswordResetTokenEntity t SET t.notified = true WHERE t.notified = false AND t.expiresAt < :now")
    int markExpiredNotified(@Param("now") Instant now);
}
