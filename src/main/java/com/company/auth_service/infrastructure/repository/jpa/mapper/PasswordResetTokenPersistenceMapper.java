package com.company.auth_service.infrastructure.repository.jpa.mapper;

import com.company.auth_service.domain.token.PasswordResetToken;
import com.company.auth_service.infrastructure.repository.jpa.entity.PasswordResetTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenPersistenceMapper {


    PasswordResetTokenEntity toEntity(PasswordResetToken token);

    PasswordResetToken toDomain(PasswordResetTokenEntity entity);
}
