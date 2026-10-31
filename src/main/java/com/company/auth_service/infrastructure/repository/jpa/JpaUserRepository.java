package com.company.auth_service.infrastructure.repository.jpa;

import com.company.auth_service.domain.PendingPasswordResetNotification;
import com.company.auth_service.domain.PendingUserNotification;
import com.company.auth_service.domain.User;

import java.util.*;

import com.company.auth_service.domain.UserRepository;
import com.company.auth_service.infrastructure.repository.UserJpaDao;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import com.company.auth_service.infrastructure.repository.jpa.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth", name = "repository-mode", havingValue = "jpa")
public class JpaUserRepository implements UserRepository{

    private final UserJpaDao dao;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return dao.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return dao.existsByEmail(email);
    }

    @Override
    public Map<UUID, String> findEmailsByIds(Collection<UUID> ids) {
        return Map.of();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PendingUserNotification> lockPendingNotifications(int limit) {
        return dao.lockPendingNotifications(limit).stream()
                .map(r -> new PendingPasswordResetNotification(r.getId(), r.getEventId(), r.getToken(), r.getUserId()))
                .toList();
    }

    @Override @Transactional
    public int markNotified(Collection<UUID> ids) {
        if (ids.isEmpty()) return 0;
        return dao.markNotified(ids);
    }

//    @Override
//    public List<User> findAllByNotifiedFalse() {
//        return dao.findAllByNotifiedFalse().stream().map(mapper::toDomain).toList();
//    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        return mapper.toDomain(dao.save(entity));
    }
}
