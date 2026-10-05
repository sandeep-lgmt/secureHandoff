package com.securehandoff.securehandoff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.securehandoff.securehandoff.model.AcessPacket;

public record CreatePacketRequest(@NotBlank(message = "Title is required") String title,
        @NotNull(message = "Category is required") AcessPacket.PacketCategory category,
        @NotBlank(message = "Content is required") String plaintextContent
        ) {

}
