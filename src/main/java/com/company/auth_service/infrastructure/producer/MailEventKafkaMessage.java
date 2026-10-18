package com.company.auth_service.infrastructure.producer;

import com.company.auth_service.domain.Locales;
import com.company.auth_service.domain.TemplateKeys;
import java.util.Map;
import lombok.Builder;

@Builder
public record MailEventKafkaMessage(
        String eventId,
        String recipient,
        TemplateKeys templateKey,
        Locales locale,
        Map<String, Object> variables
) {}


