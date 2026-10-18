package com.company.auth_service.infrastructure.repository.jpa.mapper;

import com.company.auth_service.domain.User;
import com.company.auth_service.infrastructure.repository.jpa.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    UserEntity toEntity(User user);

    User toDomain(UserEntity entity);
}
