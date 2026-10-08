package com.securehandoff.securehandoff.service;

import static com.securehandoff.securehandoff.config.KafkaConfig.ESCALATION_TOPIC;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.event.EscalationEvent;
import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.ReleaseRequestRepository;
import com.securehandoff.securehandoff.repository.TrusteeLinkRepository;
import com.securehandoff.securehandoff.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationConsumer {

    private final UserRepository userRepository;
    private final TrusteeLinkRepository trusteeLinkRepository;
    private final ReleaseRequestRepository releaseRequestRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @KafkaListener(topics = ESCALATION_TOPIC, groupId = "securehandoff-group")
    @Transactional
    public void onEscalation(EscalationEvent event) {
        log.info("Received escalation event for owner {}", event.ownerEmail());

        User owner = userRepository.findById(event.ownerId())
                .orElseThrow(() -> new IllegalStateException("Owner not found: " + event.ownerId()));

        // Idempotency: don't create a second request if one is already waiting on trustees.
        if (releaseRequestRepository.findFirstByOwnerAndStatus(owner, ReleaseRequest.Status.PENDING).isPresent()) {
            log.info("A pending release request already exists for owner {}; skipping.", owner.getEmail());
            return;
        }

        List<TrusteeLink> acceptedTrustees =
                trusteeLinkRepository.findByOwnerAndStatus(owner, TrusteeLink.TrusteeStatus.ACCEPTED);

        if (acceptedTrustees.isEmpty()) {
            log.warn("Owner {} missed check-in but has no accepted trustees; nobody to notify.", owner.getEmail());
            auditService.log(owner, "ESCALATION_FAILED_NO_TRUSTEES", "Missed check-in but no accepted trustees to notify");
            return;
        }

        int required = Math.min(2, acceptedTrustees.size());

        ReleaseRequest request = releaseRequestRepository.save(ReleaseRequest.builder()
                .owner(owner)
                .requiredConfirmations(required)
                .status(ReleaseRequest.Status.PENDING)
                .build());

        for (TrusteeLink link : acceptedTrustees) {
            notificationService.sendTrusteeEscalationAlert(link.getTrusteeEmail(), owner.getEmail(), request.getId());
        }

        auditService.log(owner, "RELEASE_REQUEST_CREATED",
                "Release request #" + request.getId() + " created, requires " + required + " trustee confirmation(s)");
    }
}
