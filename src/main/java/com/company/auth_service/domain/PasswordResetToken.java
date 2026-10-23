package com.company.auth_service.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    private UUID id;
    private User user;
    private String token;
    private Instant expiresAt;
    private boolean used;
    private String eventId;
    private boolean notified;
    private Instant createdAt;

    public static PasswordResetToken issue(User user, String token, Instant expiresAt, String eventId) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUsed(false);
        resetToken.setEventId(eventId);
        resetToken.setNotified(false);
        return resetToken;
    }

    public void markUsed() {
        this.used = true;
    }

    public void redeem(String newPasswordHash) {
        user.changePassword(newPasswordHash);
        //this.used = true;
        // Mark the token as used after redeeming V1
        markUsed(); // Mark the token as used after redeeming V2
    }

    public void markNotified() {
        this.notified = true;
    }
}


