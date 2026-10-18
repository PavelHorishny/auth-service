package com.company.auth_service.infrastructure.repository;

import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenJpaDao extends JpaRepository<PasswordResetTokenEntity, UUID> {
    Optional<PasswordResetTokenEntity> findByTokenAndUsedFalseAndExpiresAtAfter(String token, Instant now);
    @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.token = :token AND t.used = false AND t.expiresAt > :now")
    Optional<PasswordResetTokenEntity> findValidToken(@Param("token") String token, @Param("now") Instant now);
    List<PasswordResetTokenEntity> findAllByNotifiedFalse();

}
