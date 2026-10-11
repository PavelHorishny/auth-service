package com.company.auth_service.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank @Email String email
) {}

/*
Czy należy przenieść rekordy do domeny (domain), ponieważ od niczego nie zależą,
czy zostawić w warstwie web, ponieważ są logicznie powiązane z kontrolerem?*/
