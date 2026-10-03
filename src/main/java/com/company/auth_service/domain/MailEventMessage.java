package com.company.auth_service.domain;

import java.util.Map;
import lombok.Builder;

@Builder
public record MailEventMessage(
        String eventId,
        String recipient,
        String templateKey,
        String locale,
        Map<String, Object> variables
) {}

/*Żeby rekord pasował do takiego samego z serwisu mailingowego*/
