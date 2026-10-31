package com.company.auth_service.domain;

import java.util.Map;

public final class UserRegistrationMailEventFactory {

    private UserRegistrationMailEventFactory() {
    }
    public static MailEventMessage from(User user) {
        return of(user.getEventId(), user.getEmail());
    }


    public static MailEventMessage from(PendingUserNotification pending) {
        return of(pending.eventId(), pending.email());
    }

    private static MailEventMessage of(String eventId, String email) {
        return MailEventMessage.builder()
                .eventId(eventId)
                .recipient(email)
                .templateKey(TemplateKeys.NEW_USER)
                .locale(Locales.EN)
                .variables(Map.of("email", email))
                .build();
    }
}
