package com.company.auth_service.infrastructure.producer;

import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.infrastructure.producer.mapper.MailEventKafkaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth", name = "mailing-producer-mode", havingValue = "kafka")
public class KafkaMailEventPublisher implements MailEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MailEventKafkaMapper mailEventKafkaMapper;

    @Value("${auth.mailing-topic}")
    private String topic;

    @Override
    public void publish(MailEventMessage event) {
        kafkaTemplate.send(topic, event.eventId(), mailEventKafkaMapper.toKafkaMessage(event));
        //TODO add get
        log.info("Published mail event {} (template={}) to topic {}", event.eventId(), event.templateKey(), topic);
    }
}
