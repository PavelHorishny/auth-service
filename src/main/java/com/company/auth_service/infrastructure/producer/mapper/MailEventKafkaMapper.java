package com.company.auth_service.infrastructure.producer.mapper;

import com.company.auth_service.domain.MailEventMessage;
import com.company.auth_service.infrastructure.producer.MailEventKafkaMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MailEventKafkaMapper {

    MailEventKafkaMessage toKafkaMessage(MailEventMessage event);
}
