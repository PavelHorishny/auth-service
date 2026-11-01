package com.company.auth_service.domain.token;

import java.util.UUID;

public record PendingPasswordResetNotification(UUID id, String eventId, String token, UUID userId) {}
