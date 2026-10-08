package com.securehandoff.securehandoff.dto;

import java.time.Instant;

import com.securehandoff.securehandoff.model.AccessPacket;

public record PacketSummaryResponse(
        Long id,
        String title,
        AccessPacket.PacketCategory category,
        Instant createdAt,
        Instant updatedAt) {

    public static PacketSummaryResponse from(AccessPacket packet) {
        return new PacketSummaryResponse(
                packet.getId(), packet.getTitle(), packet.getCategory(),
                packet.getCreatedAt(), packet.getUpdatedAt());
    }
}
