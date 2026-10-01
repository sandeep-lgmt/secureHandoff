package com.securehandoff.securehandoff.service;

import java.time.Instant;
import java.util.stream.Collectors;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import com.securehandoff.securehandoff.model.ReleaseConfirmation;
import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.repository.AccessPacketRepository;
import com.securehandoff.securehandoff.repository.TrusteeLinkRepository;

public class ConsensuService {
    private final ReleaseRequestRepository releaseRequestRepository;
    private final ReleaseConfirmationRepository confirmationRepository;
    private final TrusteeLinkRepository trusteeLinkRepository;
    private final AccessPacketRepository packetRepository;
    private final PacketService packetService;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * A trustee submits their confirmation. Returns the updated ReleaseRequest.
     * Throws if: the request doesn't exist, isn't PENDING, the caller isn't an
     * ACCEPTED trustee for that owner, or the caller already confirmed (no double-counting).
     */
    @Transactional
    public ReleaseRequest confirm(Long releaseRequestId, User confirmingTrustee) {
        ReleaseRequest request = releaseRequestRepository.findById(releaseRequestId)
                .orElseThrow(() -> new ApiException("Release request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != ReleaseRequest.Status.PENDING) {
            throw new ApiException("This release request is no longer pending", HttpStatus.CONFLICT);
        }

        boolean isAcceptedTrustee = trusteeLinkRepository.findByOwnerAndStatus(request.getOwner(), TrusteeLink.TrusteeStatus.ACCEPTED)
                .stream()
                .anyMatch(link -> link.getTrusteeUser() != null && link.getTrusteeUser().getId().equals(confirmingTrustee.getId()));

        if (!isAcceptedTrustee) {
            throw new ApiException("You are not an accepted trustee for this owner", HttpStatus.FORBIDDEN);
        }

        boolean alreadyConfirmed = confirmationRepository
                .findByReleaseRequestAndTrustee(request, confirmingTrustee)
                .isPresent();
        if (alreadyConfirmed) {
            throw new ApiException("You have already confirmed this release request", HttpStatus.CONFLICT);
        }

        ReleaseConfirmation confirmation = ReleaseConfirmation.builder()
                .releaseRequest(request)
                .trustee(confirmingTrustee)
                .build();
        confirmationRepository.save(confirmation);

        auditService.log(confirmingTrustee, "RELEASE_CONFIRMED",
                "Confirmed release request #" + request.getId() + " for owner " + request.getOwner().getEmail());

        long totalConfirmations = confirmationRepository.countByReleaseRequest(request);

        if (totalConfirmations >= request.getRequiredConfirmations()) {
            request.setStatus(ReleaseRequest.Status.QUORUM_MET);
            request.setQuorumMetAt(Instant.now());
            releaseRequestRepository.save(request);

            auditService.log(request.getOwner(), "RELEASE_QUORUM_MET",
                    "Release request #" + request.getId() + " reached quorum (" + totalConfirmations + "/" + request.getRequiredConfirmations() + ")");

            kafkaTemplate.send(RELEASE_TOPIC, request.getOwner().getId().toString(),
                    new ReleaseEvent(request.getOwner().getId(), request.getId(), Instant.now()));
        }

        return request;
    }

    /**
     * Returns decrypted packet contents — ONLY reachable if the ReleaseRequest has
     * already reached QUORUM_MET, and ONLY to a caller who is an accepted trustee
     * for that owner. This is the single point in the entire codebase where plaintext
     * is produced for external consumption.
     */
    @Transactional
    public Map<String, String> getReleasedContent(Long releaseRequestId, User requestingTrustee) {
        ReleaseRequest request = releaseRequestRepository.findById(releaseRequestId)
                .orElseThrow(() -> new ApiException("Release request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != ReleaseRequest.Status.QUORUM_MET) {
            throw new ApiException("Quorum has not been reached yet for this release request", HttpStatus.FORBIDDEN);
        }

        boolean isAcceptedTrustee = trusteeLinkRepository.findByOwnerAndStatus(request.getOwner(), TrusteeLink.TrusteeStatus.ACCEPTED)
                .stream()
                .anyMatch(link -> link.getTrusteeUser() != null && link.getTrusteeUser().getId().equals(requestingTrustee.getId()));

        if (!isAcceptedTrustee) {
            throw new ApiException("You are not an accepted trustee for this owner", HttpStatus.FORBIDDEN);
        }

        List<AccessPacket> packets = packetRepository.findByOwner(request.getOwner());

        auditService.log(requestingTrustee, "PACKETS_ACCESSED",
                "Accessed released packets for owner " + request.getOwner().getEmail() + " under release request #" + request.getId());

        // Two packets may share a title; toMap without a merge function would throw -> HTTP 500.
        return packets.stream().collect(Collectors.toMap(
                p -> p.getTitle() + " (#" + p.getId() + ")",
                packetService::decryptForLegitimateRelease,
                (a, b) -> a,
                java.util.LinkedHashMap::new
        ));
    }

}
