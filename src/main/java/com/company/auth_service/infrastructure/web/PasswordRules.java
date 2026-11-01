package com.company.auth_service.infrastructure.web;

public class PasswordRules {
    static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[\\x20-\\x7E]{8,72}$";
    static final String MESSAGE =
            "Password must be 8-72 characters (Latin letters, digits, symbols) and contain a letter and a digit";

    private PasswordRules() {
    }
}
