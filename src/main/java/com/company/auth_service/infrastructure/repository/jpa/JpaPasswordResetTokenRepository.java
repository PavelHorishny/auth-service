package com.company.auth_service.infrastructure.repository.jpa;

import com.company.auth_service.domain.PasswordResetToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.company.auth_service.domain.PasswordResetTokenRepository;
import com.company.auth_service.domain.PendingPasswordResetNotification;
import com.company.auth_service.infrastructure.repository.TokenJpaDao;
import com.company.auth_service.infrastructure.repository.UserJpaDao;
import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import com.company.auth_service.infrastructure.repository.jpa.mapper.PasswordResetTokenPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth", name = "repository-mode", havingValue = "jpa")
public class JpaPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final TokenJpaDao dao;
    private final UserJpaDao userDao;
    private final PasswordResetTokenPersistenceMapper mapper;

    @Override
    public Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, Instant now) {
        return dao.findByTokenAndUsedFalseAndExpiresAtAfter(token, now).map(mapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PendingPasswordResetNotification> lockPendingNotifications(int limit) {
        return dao.lockPendingNotifications(limit).stream()
                .map(r -> new PendingPasswordResetNotification(r.getId(), r.getEventId(), r.getToken(), r.getUserId()))
                .toList();
    }

    @Override @Transactional
    public int markNotified(Collection<UUID> ids) {
        if (ids.isEmpty()) return 0;
        return dao.markNotified(ids);
    }

    @Override
    public int markExpiredNotified(Instant now) {
        return 0;
    }

//    @Override
//    public List<PasswordResetToken> findAllByNotifiedFalse() {
//        return dao.findAllByNotifiedFalse().stream().map(mapper::toDomain).toList();
//    }

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        PasswordResetTokenEntity entity = mapper.toEntity(passwordResetToken);
        entity.setUser(userDao.getReferenceById(passwordResetToken.getUser().getId()));
        return mapper.toDomain(dao.save(entity));
    }
}
