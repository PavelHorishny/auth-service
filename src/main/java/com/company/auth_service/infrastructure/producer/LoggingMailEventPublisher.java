package com.company.auth_service.infrastructure.producer;

import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "auth", name = "mailing-producer-mode", havingValue = "mock")
public class LoggingMailEventPublisher implements MailEventPublisher {

    @Override
    public void publish(MailEventMessage event) {
        log.info("[mock] would publish mail event {} (template={}, recipient={})",
                event.eventId(), event.templateKey(), event.recipient());
    }

/*
    Implementacja publishera do celów testowych, która nie wysyła wiadomości przez Kafkę, lecz loguje, jaka wiadomość miała zostać wysłana.
*/
}
