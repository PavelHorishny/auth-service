package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaDao extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    //List<UserEntity> findAllByNotifiedFalse();
    @Query(value = """
        SELECT id AS "id", event_id AS "eventId", email AS "email"
        FROM users WHERE notified = false
        ORDER BY created_at LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PendingNotificationRow> lockPendingNotifications(@Param("limit") int limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.notified = true, u.updatedAt = :now WHERE u.id IN :ids AND u.notified = false")
    int markNotified(@Param("ids") Collection<UUID> ids);

    @Query("SELECT u.id AS id, u.email AS email FROM UserEntity u WHERE u.id IN :ids")
    List<EmailRow> findEmailsByIds(@Param("ids") Collection<UUID> ids);
}
