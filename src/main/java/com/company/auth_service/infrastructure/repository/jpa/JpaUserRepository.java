package com.company.auth_service.infrastructure.repository.jpa;

import com.company.auth_service.domain.User;

import java.util.List;
import java.util.Optional;

import com.company.auth_service.domain.UserRepository;
import com.company.auth_service.infrastructure.repository.UserJpaDao;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import com.company.auth_service.infrastructure.repository.jpa.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

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
    public List<User> findAllByNotifiedFalse() {
        return dao.findAllByNotifiedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        return mapper.toDomain(dao.save(entity));
    }
}
