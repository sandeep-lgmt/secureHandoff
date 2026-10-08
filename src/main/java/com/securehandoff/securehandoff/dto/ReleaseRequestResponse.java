package com.securehandoff.securehandoff.dto;

import java.time.Instant;

import com.securehandoff.securehandoff.model.ReleaseRequest;

public record ReleaseRequestResponse(
        Long id,
        String ownerEmail,
        int requiredConfirmations,
        ReleaseRequest.Status status,
        Instant createdAt,
        Instant quorumMetAt) {

    public static ReleaseRequestResponse from(ReleaseRequest request) {
        return new ReleaseRequestResponse(
                request.getId(),
                request.getOwner().getEmail(),
                request.getRequiredConfirmations(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getQuorumMetAt());
    }
}
