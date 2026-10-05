package com.securehandoff.securehandoff.dto;

import java.time.Instant;

import com.securehandoff.securehandoff.model.ReleaseRequest;

public record RelaseRequestResponse(
        Long id,
        String ownerEmail,
        int requiredConfirmations,
        ReleaseRequest.Status status,
        Instant createdAt,
        Instant quorumMetAt
) {
      public static ReleaseRequestResponse from(ReleaseRequest r) {
        return new ReleaseRequestResponse(
                r.getId(), r.getOwner().getEmail(), r.getRequiredConfirmations(),
                r.getStatus(), r.getCreatedAt(), r.getQuorumMetAt()
        );
    }
}
