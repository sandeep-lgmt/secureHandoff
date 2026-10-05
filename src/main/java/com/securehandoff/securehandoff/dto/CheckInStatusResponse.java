package com.securehandoff.securehandoff.dto;

import java.time.Instant;
import com.securehandoff.securehandoff.model.CheckInConfig;
 

public record CheckInStatusResponse( int frequencyDays,
        Instant lastCheckInAt,
        CheckInConfig.CheckInStatus status) {

        public static CheckInStatusResponse from(CheckInConfig config) {
        return new CheckInStatusResponse(config.getFrequencyDays(), config.getLastCheckInAt(), config.getStatus());
    }

}
