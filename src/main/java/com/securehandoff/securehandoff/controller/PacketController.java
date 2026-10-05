package com.securehandoff.securehandoff.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.securehandoff.securehandoff.service.PacketService;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/packets")
@RequiredArgsConstructor
public class PacketController {
     private final PacketService packetService;

    @PostMapping
    public ResponseEntity<PacketSummaryResponse> create(
            @AuthenticationPrincipal User owner,
            @Valid @RequestBody CreatePacketRequest request
    ) {
        var packet = packetService.createPacket(owner, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacketSummaryResponse.from(packet));
    }

    @GetMapping
    public ResponseEntity<List<PacketSummaryResponse>> listMine(@AuthenticationPrincipal User owner) {
        var packets = packetService.listMyPackets(owner).stream().map(PacketSummaryResponse::from).toList();
        return ResponseEntity.ok(packets);
    }

    @GetMapping("/{packetId}")
    public ResponseEntity<PacketSummaryResponse> getOne(
            @AuthenticationPrincipal User owner,
            @PathVariable Long packetId
    ) {
        var packet = packetService.getMyPacketSummary(owner, packetId);
        return ResponseEntity.ok(PacketSummaryResponse.from(packet));
    }

    @PutMapping("/{packetId}")
    public ResponseEntity<PacketSummaryResponse> update(
            @AuthenticationPrincipal User owner,
            @PathVariable Long packetId,
            @Valid @RequestBody UpdatePacketRequest request
    ) {
        var packet = packetService.updatePacket(owner, packetId, request);
        return ResponseEntity.ok(PacketSummaryResponse.from(packet));
    }

    @DeleteMapping("/{packetId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User owner, @PathVariable Long packetId) {
        packetService.deletePacket(owner, packetId);
        return ResponseEntity.noContent().build();
    }

}
