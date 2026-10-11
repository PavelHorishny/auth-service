package com.company.auth_service.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword
) {}
