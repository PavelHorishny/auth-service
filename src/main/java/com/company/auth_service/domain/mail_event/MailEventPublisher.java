package com.company.auth_service.domain.mail_event;

public interface MailEventPublisher {
    void publish(MailEventMessage event);
}
