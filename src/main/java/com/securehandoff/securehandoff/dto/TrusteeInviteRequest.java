package com.securehandoff.securehandoff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TrusteeInviteRequest(
    @NotBlank @Email(message = "A valid trustee email is required") String trusteeEmail) {

}
