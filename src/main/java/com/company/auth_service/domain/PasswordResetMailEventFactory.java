package com.company.auth_service.domain;

import java.util.Map;

public final class PasswordResetMailEventFactory {

    private PasswordResetMailEventFactory() {
    }
    public static MailEventMessage from(PasswordResetToken resetToken, String recipientEmail) {
        return of(resetToken.getEventId(), recipientEmail, resetToken.getToken());
    }
    public static MailEventMessage from(PendingPasswordResetNotification pending, String recipientEmail) {
        return of(pending.eventId(), recipientEmail, pending.token());
    }
    public static MailEventMessage of(String eventId, String recipientEmail, String resetToken) {
        return MailEventMessage.builder()
                .eventId(eventId)
                .recipient(recipientEmail)
                .templateKey(TemplateKeys.PASSWORD_RESET)
                .locale(Locales.EN)
                .variables(Map.of("resetToken", resetToken))
                .build();
    }
}
