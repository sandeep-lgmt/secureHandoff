package com.securehandoff.securehandoff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank @Email(message = "A valid email is required") String email,
        // BCrypt only uses the first 72 bytes, so cap the length.
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {
}
