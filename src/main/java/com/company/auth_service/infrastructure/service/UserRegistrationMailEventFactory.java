package com.company.auth_service.infrastructure.service;

import com.company.auth_service.domain.Locales;
import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.TemplateKeys;
import com.company.auth_service.domain.User;
import java.util.Map;

public final class UserRegistrationMailEventFactory {

    private UserRegistrationMailEventFactory() {
    }

    public static MailEventMessage from(User user) {
        return MailEventMessage.builder()
                .eventId(user.getEventId())
                .recipient(user.getEmail())
                .templateKey(TemplateKeys.NEW_USER)
                .locale(Locales.EN)
                .variables(Map.of("email", user.getEmail()))
                .build();
    }
}
