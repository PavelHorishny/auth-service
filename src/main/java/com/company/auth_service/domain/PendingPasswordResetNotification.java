package com.company.auth_service.domain;

import java.util.UUID;

public record PendingPasswordResetNotification(UUID id, String eventId, String token, UUID userId) {}
