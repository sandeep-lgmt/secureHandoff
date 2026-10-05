package com.securehandoff.securehandoff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SetupCheckInRequest(
     @Min(value = 1, message = "Frequency must be at least 1 day")
     @Max(value = 365, message = "Frequency must be under a year") 
     int frequencyDays
) {

}
