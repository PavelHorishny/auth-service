package com.company.auth_service.app;

import com.company.auth_service.domain.PasswordResetService;
import com.company.auth_service.infrastructure.web.ConfirmResetPasswordRequest;
import com.company.auth_service.infrastructure.web.ResetPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;


    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmResetPassword(@Valid @RequestBody ConfirmResetPasswordRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
