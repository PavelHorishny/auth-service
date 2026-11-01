package com.company.auth_service.domain.mailevent;

public interface MailEventPublisher {
    void publish(MailEventMessage event);
}
