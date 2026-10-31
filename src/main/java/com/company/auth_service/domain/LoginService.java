package com.company.auth_service.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final AccessTokenGenerator accessTokenGenerator;

    public String login(String email, String rawPassword){
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, rawPassword));
        return accessTokenGenerator.generateToken(authentication.getName());
    }
}
