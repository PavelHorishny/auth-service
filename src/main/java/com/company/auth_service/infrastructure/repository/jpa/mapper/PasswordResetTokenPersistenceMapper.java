package com.company.auth_service.infrastructure.repository.jpa.mapper;

import com.company.auth_service.domain.PasswordResetToken;
import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserPersistenceMapper.class)
public interface PasswordResetTokenPersistenceMapper {

    @Mapping(target = "user", ignore = true)
    PasswordResetTokenEntity toEntity(PasswordResetToken token);

    PasswordResetToken toDomain(PasswordResetTokenEntity entity);
}
