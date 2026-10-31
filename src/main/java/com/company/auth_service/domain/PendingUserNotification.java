package com.company.auth_service.domain;

import java.util.UUID;

public record PendingUserNotification(UUID id, String eventId, String email) {}
