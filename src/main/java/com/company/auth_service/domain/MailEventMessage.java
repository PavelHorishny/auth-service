package com.company.auth_service.domain;

import java.util.Map;
import lombok.Builder;

@Builder
public record MailEventMessage(
        String eventId,
        String recipient,
        TemplateKeys templateKey,
        Locales locale,
        Map<String, Object> variables
) {}


