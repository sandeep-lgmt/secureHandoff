package com.securehandoff.securehandoff.event;

import java.io.Serializable;
import java.time.Instant;

public record RelaseEvent(
    Long ownerId, 
    Long releaseRequestId, 
    Instant releasedAt) implements Serializable {

}
