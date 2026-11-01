package com.company.auth_service.domain.token;

public interface AccessTokenGenerator {
    String generateToken(String email);
}
