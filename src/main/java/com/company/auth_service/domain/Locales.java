package com.company.auth_service.domain;

public enum Locales {
    EN("en"),
    RU("ru"),
    PL("pl");

    private final String code;

    Locales(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
