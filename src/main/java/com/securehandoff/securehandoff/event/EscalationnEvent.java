package com.securehandoff.securehandoff.event;

import java.io.Serializable;
import java.time.Instant;

public record EscalationnEvent(
    Long ownerId, 
    String ownerEmail, 
    Instant triggeredAt) implements Serializable {

}
