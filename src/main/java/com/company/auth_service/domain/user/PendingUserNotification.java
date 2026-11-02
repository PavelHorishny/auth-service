package com.company.auth_service.domain.user;

import java.util.UUID;

public record PendingUserNotification(UUID id, String eventId, String email) {}
