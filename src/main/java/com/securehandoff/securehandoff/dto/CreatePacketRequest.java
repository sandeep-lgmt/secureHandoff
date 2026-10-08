package com.securehandoff.securehandoff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.securehandoff.securehandoff.model.AccessPacket;

public record CreatePacketRequest(@NotBlank(message = "Title is required") String title,
        @NotNull(message = "Category is required") AccessPacket.PacketCategory category,
        @NotBlank(message = "Content is required") String plaintextContent
        ) {

}
