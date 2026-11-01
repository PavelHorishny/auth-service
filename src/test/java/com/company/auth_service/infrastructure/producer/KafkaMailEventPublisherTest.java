package com.company.auth_service.infrastructure.producer;

import com.company.auth_service.domain.exceptions.MailEventPublishException;
import com.company.auth_service.domain.mailevent.Locales;
import com.company.auth_service.domain.mailevent.MailEventMessage;
import com.company.auth_service.domain.mailevent.TemplateKeys;
import com.company.auth_service.infrastructure.producer.mapper.MailEventKafkaMapper;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMailEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private MailEventKafkaMapper mailEventKafkaMapper;

    private KafkaMailEventPublisher publisher;

    private final MailEventMessage event = MailEventMessage.builder()
            .eventId("event-1")
            .recipient("user@example.com")
            .templateKey(TemplateKeys.NEW_USER)
            .locale(Locales.EN)
            .variables(Map.of())
            .build();

    @BeforeEach
    void setUp() {
        publisher = new KafkaMailEventPublisher(kafkaTemplate, mailEventKafkaMapper);
        ReflectionTestUtils.setField(publisher, "topic", "mailing-events");
        ReflectionTestUtils.setField(publisher, "sendTimeoutMs", 200L);
        when(mailEventKafkaMapper.toKafkaMessage(event))
                .thenReturn(MailEventKafkaMessage.builder().eventId("event-1").build());
    }

    @Test
    void publish_returnsOnceTheBrokerHasAcknowledged() {
        when(kafkaTemplate.send(eq("mailing-events"), eq("event-1"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publish_throwsWhenTheBrokerRejectsTheRecord() {
        when(kafkaTemplate.send(eq("mailing-events"), eq("event-1"), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("not enough in-sync replicas")));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(MailEventPublishException.class)
                .hasRootCauseInstanceOf(KafkaException.class);
    }

    @Test
    void publish_throwsWhenNoAcknowledgementArrivesInTime() {
        when(kafkaTemplate.send(eq("mailing-events"), eq("event-1"), any()))
                .thenReturn(new CompletableFuture<>());          // никогда не завершится

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(MailEventPublishException.class);
    }
}