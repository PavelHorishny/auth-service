package com.company.auth_service.domain.mailevent;

import lombok.Getter;

@Getter
public enum Locales {
    EN("en"),
    RU("ru"),
    PL("pl");

    private final String code;

    Locales(String code) {
        this.code = code;
    }

}
