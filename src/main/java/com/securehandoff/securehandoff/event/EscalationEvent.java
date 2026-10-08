package com.securehandoff.securehandoff.event;

import java.time.Instant;

public record EscalationEvent(Long ownerId, String ownerEmail, Instant triggeredAt) {
}
