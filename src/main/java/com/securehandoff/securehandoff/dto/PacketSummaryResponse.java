package com.securehandoff.securehandoff.dto;
import com.securehandoff.securehandoff.model.AcessPacket;
import java.time.Instant;

public record PacketSummaryResponse(
        Long id,
        String title,
        AcessPacket.PacketCategory category,
        Instant createdAt,
        Instant updatedAt) {
             public static PacketSummaryResponse from(AcessPacket packet) {
        return new PacketSummaryResponse(
                packet.getId(), packet.getTitle(), packet.getCategory(),
                packet.getCreatedAt(), packet.getUpdatedAt()
        );
    }

}
