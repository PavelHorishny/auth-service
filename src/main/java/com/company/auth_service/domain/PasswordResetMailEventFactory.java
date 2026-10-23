package com.company.auth_service.domain;

import java.util.Map;

public final class PasswordResetMailEventFactory {

    private PasswordResetMailEventFactory() {
    }

    public static MailEventMessage from(PasswordResetToken resetToken) {
        return MailEventMessage.builder()
                .eventId(resetToken.getEventId())
                .recipient(resetToken.getUser().getEmail())
                .templateKey(TemplateKeys.PASSWORD_RESET)
                .locale(Locales.EN)
                .variables(Map.of("resetToken", resetToken.getToken()))
                .build();
    }
    //move to domain?
}
