package com.company.auth_service.domain.auth;

public interface AccessTokenGenerator {
    String generateToken(String email);
}
