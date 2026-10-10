package com.company.auth_service.domain;

import lombok.Getter;

@Getter
public enum TemplateKeys {
    NONE("none"),
    NEW_USER("new_user"),
    PASSWORD_RESET("password_reset");


    private final String template;

    TemplateKeys(String template) {
        this.template = template;
    }
}
