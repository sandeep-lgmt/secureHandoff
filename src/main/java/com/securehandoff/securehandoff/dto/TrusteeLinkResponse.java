package com.securehandoff.securehandoff.dto;

import java.time.Instant;

import com.securehandoff.securehandoff.model.TrusteeLink;

public record TrusteeLinkResponse(
        Long id,
        String trusteeEmail,
        TrusteeLink.TrusteeStatus status,
        Instant createdAt
) {
     public static TrusteeLinkResponse from(TrusteeLink link) {
        return new TrusteeLinkResponse(link.getId(), link.getTrusteeEmail(), link.getStatus(), link.getCreatedAt());
    }

}
