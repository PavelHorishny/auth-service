package com.company.auth_service.infrastructure.repository;

public interface PendingNotificationRow {
    UUID getId(); String getEventId(); String getEmail();
}
