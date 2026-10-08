package com.securehandoff.securehandoff.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.dto.ReleaseRequestResponse;
import com.securehandoff.securehandoff.event.ReleaseEvent;
import com.securehandoff.securehandoff.exception.ApiException;
import com.securehandoff.securehandoff.model.AccessPacket;
import com.securehandoff.securehandoff.model.ReleaseConfirmation;
import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.AccessPacketRepository;
import com.securehandoff.securehandoff.repository.ReleaseConfirmationRepository;
import com.securehandoff.securehandoff.repository.ReleaseRequestRepository;
import com.securehandoff.securehandoff.repository.TrusteeLinkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsensusService {

    private final ReleaseRequestRepository releaseRequestRepository;
    private final ReleaseConfirmationRepository confirmationRepository;
    private final TrusteeLinkRepository trusteeLinkRepository;
    private final AccessPacketRepository packetRepository;
    private final PacketService packetService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /** Release requests currently waiting on this trustee. */
    @Transactional(readOnly = true)
    public List<ReleaseRequestResponse> listPendingFor(User trustee) {
        List<User> owners = trusteeLinkRepository.findByTrusteeUser(trustee).stream()
                .filter(link -> link.getStatus() == TrusteeLink.TrusteeStatus.ACCEPTED)
                .map(TrusteeLink::getOwner)
                .toList();

        if (owners.isEmpty()) {
            return List.of();
        }

        return releaseRequestRepository.findByOwnerInAndStatus(owners, ReleaseRequest.Status.PENDING).stream()
                .map(ReleaseRequestResponse::from)
                .toList();
    }

    @Transactional
    public ReleaseRequestResponse confirm(Long releaseRequestId, User confirmingTrustee) {
        // Row lock: simultaneous confirmations queue here, so the count below is always accurate.
        ReleaseRequest request = releaseRequestRepository.findByIdForUpdate(releaseRequestId)
                .orElseThrow(() -> new ApiException("Release request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != ReleaseRequest.Status.PENDING) {
            throw new ApiException("This release request is no longer pending", HttpStatus.CONFLICT);
        }

        requireAcceptedTrustee(request.getOwner(), confirmingTrustee);

        if (confirmationRepository.findByReleaseRequestAndTrustee(request, confirmingTrustee).isPresent()) {
            throw new ApiException("You have already confirmed this release request", HttpStatus.CONFLICT);
        }

        confirmationRepository.save(ReleaseConfirmation.builder()
                .releaseRequest(request)
                .trustee(confirmingTrustee)
                .build());

        auditService.log(confirmingTrustee, "RELEASE_CONFIRMED",
                "Confirmed release request #" + request.getId() + " for owner " + request.getOwner().getEmail());

        long totalConfirmations = confirmationRepository.countByReleaseRequest(request);

        if (totalConfirmations >= request.getRequiredConfirmations()) {
            request.setStatus(ReleaseRequest.Status.QUORUM_MET);
            request.setQuorumMetAt(Instant.now());
            releaseRequestRepository.save(request);

            auditService.log(request.getOwner(), "RELEASE_QUORUM_MET",
                    "Release request #" + request.getId() + " reached quorum ("
                            + totalConfirmations + "/" + request.getRequiredConfirmations() + ")");

            // Forwarded to Kafka only after this transaction commits (see KafkaEventRelay).
            eventPublisher.publishEvent(new ReleaseEvent(request.getOwner().getId(), request.getId(), Instant.now()));
        }

        return ReleaseRequestResponse.from(request);
    }

    /**
     * Returns decrypted packet content ONLY if quorum has been met, and only for an
     * accepted trustee of that owner. Every access is written to the audit trail.
     */
    @Transactional
    public Map<String, String> getReleasedContent(Long releaseRequestId, User requestingTrustee) {
        ReleaseRequest request = releaseRequestRepository.findById(releaseRequestId)
                .orElseThrow(() -> new ApiException("Release request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != ReleaseRequest.Status.QUORUM_MET) {
            throw new ApiException("Quorum has not been reached yet for this release request", HttpStatus.FORBIDDEN);
        }

        requireAcceptedTrustee(request.getOwner(), requestingTrustee);

        List<AccessPacket> packets = packetRepository.findByOwner(request.getOwner());

        auditService.log(requestingTrustee, "PACKETS_ACCESSED",
                "Accessed released packets for owner " + request.getOwner().getEmail()
                        + " under release request #" + request.getId());

        return packets.stream().collect(Collectors.toMap(
                packet -> packet.getTitle() + " (#" + packet.getId() + ")",
                packetService::decryptForLegitimateRelease,
                (first, second) -> first,
                LinkedHashMap::new));
    }

    private void requireAcceptedTrustee(User owner, User candidate) {
        boolean isAccepted = trusteeLinkRepository.findByOwnerAndStatus(owner, TrusteeLink.TrusteeStatus.ACCEPTED).stream()
                .anyMatch(link -> link.getTrusteeUser() != null
                        && link.getTrusteeUser().getId().equals(candidate.getId()));

        if (!isAccepted) {
            throw new ApiException("You are not an accepted trustee for this owner", HttpStatus.FORBIDDEN);
        }
    }
}
