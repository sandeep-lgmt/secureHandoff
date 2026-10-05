package com.securehandoff.securehandoff.dto;

public record AuthResponse (
      String accessToken,
        String tokenType,
        String email,
        String fullName
) {
    public static AuthResponse of(String token, String email, String fullName) {
        return new AuthResponse(token, "Bearer", email, fullName);
    }

}
