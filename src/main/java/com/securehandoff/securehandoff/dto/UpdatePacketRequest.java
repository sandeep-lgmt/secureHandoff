package com.securehandoff.securehandoff.dto;

import com.securehandoff.securehandoff.model.AcessPacket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePacketRequest(
        @NotBlank(message = "Title is required") String title,
        @NotNull(message = "Category is required") AcessPacket.PacketCategory category,
        @NotBlank(message = "Content is required") String plaintextContent
) {

}
