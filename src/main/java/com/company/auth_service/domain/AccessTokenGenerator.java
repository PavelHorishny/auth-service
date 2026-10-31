package com.company.auth_service.domain;

public interface AccessTokenGenerator {
    String generateToken(String email);
}
