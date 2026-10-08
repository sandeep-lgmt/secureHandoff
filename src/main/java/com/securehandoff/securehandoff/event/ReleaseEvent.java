package com.securehandoff.securehandoff.event;

import java.time.Instant;

public record ReleaseEvent(Long ownerId, Long releaseRequestId, Instant releasedAt) {
}
