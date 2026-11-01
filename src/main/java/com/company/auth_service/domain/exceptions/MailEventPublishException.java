package com.company.auth_service.domain.exceptions;

public class MailEventPublishException extends RuntimeException {
    public MailEventPublishException(String eventId, Throwable cause) {
        super("Failed to publish mail event " + eventId, cause);
    }
}
