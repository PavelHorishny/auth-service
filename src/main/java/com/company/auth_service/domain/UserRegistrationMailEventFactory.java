package com.company.auth_service.domain;

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
