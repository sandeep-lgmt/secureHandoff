package com.securehandoff.securehandoff.service;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.http.HttpStatus;

import com.securehandoff.securehandoff.repository.AccessPacketRepository;
import com.securehandoff.dto.CreatePacketRequest;
import com.securehandoff.dto.UpdatePacketRequest;
import com.securehandoff.exception.ApiException;
import com.securehandoff.model.AccessPacket;
import com.securehandoff.model.User;
import com.securehandoff.repository.AccessPacketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PacketService {

      private final AccessPacketRepository packetRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    public AccessPacket createPacket(User owner, CreatePacketRequest request) {
        var encrypted = encryptionService.encrypt(request.plaintextContent());

        AccessPacket packet = AccessPacket.builder()
                .owner(owner)
                .title(request.title())
                .category(request.category())
                .encryptedContent(encrypted.ciphertextBase64())
                .iv(encrypted.ivBase64())
                .build();

        packet = packetRepository.save(packet);
        auditService.log(owner, "PACKET_CREATED", "Packet '" + packet.getTitle() + "' created");
        return packet;
    }

    public List<AccessPacket> listMyPackets(User owner) {
        return packetRepository.findByOwner(owner);
    }

    public AccessPacket getMyPacketSummary(User owner, Long packetId) {
        // Note: returns the entity for metadata display only. Callers must use
        // PacketSummaryResponse.from(...) — never surface encryptedContent/iv directly.
        return packetRepository.findByIdAndOwner(packetId, owner)
                .orElseThrow(() -> new ApiException("Packet not found", HttpStatus.NOT_FOUND));
    }

    public AccessPacket updatePacket(User owner, Long packetId, UpdatePacketRequest request) {
        AccessPacket packet = packetRepository.findByIdAndOwner(packetId, owner)
                .orElseThrow(() -> new ApiException("Packet not found", HttpStatus.NOT_FOUND));

        var encrypted = encryptionService.encrypt(request.plaintextContent());
        packet.setTitle(request.title());
        packet.setCategory(request.category());
        packet.setEncryptedContent(encrypted.ciphertextBase64());
        packet.setIv(encrypted.ivBase64());

        packet = packetRepository.save(packet);
        auditService.log(owner, "PACKET_UPDATED", "Packet '" + packet.getTitle() + "' updated");
        return packet;
    }

    public void deletePacket(User owner, Long packetId) {
        AccessPacket packet = packetRepository.findByIdAndOwner(packetId, owner)
                .orElseThrow(() -> new ApiException("Packet not found", HttpStatus.NOT_FOUND));

        packetRepository.delete(packet);
        auditService.log(owner, "PACKET_DELETED", "Packet '" + packet.getTitle() + "' deleted");
    }

    /**
     * DELIBERATELY PACKAGE-PRIVATE / NOT EXPOSED VIA ANY CONTROLLER YET.
     * This is the only method that ever produces plaintext, and in Phase 4 it will only
     * ever be called from ConsensusService, AFTER quorum confirmation has been verified —
     * never directly from a REST endpoint reachable by an owner or trustee.
     */
    String decryptForLegitimateRelease(AccessPacket packet) {
        return encryptionService.decrypt(packet.getEncryptedContent(), packet.getIv());
    }

}
