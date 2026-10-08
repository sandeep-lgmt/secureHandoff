package com.securehandoff.securehandoff.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.dto.CreatePacketRequest;
import com.securehandoff.securehandoff.dto.UpdatePacketRequest;
import com.securehandoff.securehandoff.exception.ApiException;
import com.securehandoff.securehandoff.model.AccessPacket;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.AccessPacketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacketService {

    private final AccessPacketRepository packetRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    @Transactional
    public AccessPacket createPacket(User owner, CreatePacketRequest request) {
        EncryptionService.EncryptedPayload encrypted = encryptionService.encrypt(request.plaintextContent());

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

    @Transactional(readOnly = true)
    public List<AccessPacket> listMyPackets(User owner) {
        return packetRepository.findByOwner(owner);
    }

    @Transactional(readOnly = true)
    public AccessPacket getMyPacketSummary(User owner, Long packetId) {
        return findOwned(owner, packetId);
    }

    @Transactional
    public AccessPacket updatePacket(User owner, Long packetId, UpdatePacketRequest request) {
        AccessPacket packet = findOwned(owner, packetId);
        EncryptionService.EncryptedPayload encrypted = encryptionService.encrypt(request.plaintextContent());

        packet.setTitle(request.title());
        packet.setCategory(request.category());
        packet.setEncryptedContent(encrypted.ciphertextBase64());
        packet.setIv(encrypted.ivBase64());

        packet = packetRepository.save(packet);
        auditService.log(owner, "PACKET_UPDATED", "Packet '" + packet.getTitle() + "' updated");
        return packet;
    }

    @Transactional
    public void deletePacket(User owner, Long packetId) {
        AccessPacket packet = findOwned(owner, packetId);
        packetRepository.delete(packet);
        auditService.log(owner, "PACKET_DELETED", "Packet '" + packet.getTitle() + "' deleted");
    }

    /**
     * Package-private on purpose: only code in this package (ConsensusService) may call it,
     * so decryption can never be reached from a controller directly.
     */
    String decryptForLegitimateRelease(AccessPacket packet) {
        return encryptionService.decrypt(packet.getEncryptedContent(), packet.getIv());
    }

    private AccessPacket findOwned(User owner, Long packetId) {
        return packetRepository.findByIdAndOwner(packetId, owner)
                .orElseThrow(() -> new ApiException("Packet not found", HttpStatus.NOT_FOUND));
    }
}
