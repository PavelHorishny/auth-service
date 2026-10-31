package com.company.auth_service.infrastructure.security;

import com.company.auth_service.domain.AccessTokenGenerator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements AccessTokenGenerator {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtTokenProvider(@Value("${auth.jwt.secret}") String secret,
                            @Value("${auth.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String generateToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

/*
    To jest komponent, który potrafi robić z JWT dwie rzeczy: tworzyć token (podczas logowania) oraz sprawdzać/odczytywać go (przy każdym żądaniu w filtrze)
*/
}
