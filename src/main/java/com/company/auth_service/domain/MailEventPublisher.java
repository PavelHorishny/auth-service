package com.company.auth_service.domain;

public interface MailEventPublisher {
    void publish(MailEventMessage event);
}
