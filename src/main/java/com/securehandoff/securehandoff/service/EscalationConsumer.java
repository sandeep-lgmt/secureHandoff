package com.securehandoff.securehandoff.service;

import org.springframework.kafka.annotation.KafkaListener;

import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.repository.TrusteeLinkRepository;
import com.securehandoff.securehandoff.repository.UserRepository;
import com.securehandoff.securehandoff.event.EscalationEvent;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.ReleaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.securehandoff.config.KafkaConfig.ESCALATION_TOPIC;

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
    public void onEscalation(EscalationEvent event) {
        log.info("Received escalation event for owner {}", event.ownerEmail());

        User owner = userRepository.findById(event.ownerId())
                .orElseThrow(() -> new IllegalStateException("Owner not found: " + event.ownerId()));

        // Idempotency guard: don't create a duplicate PENDING request if one already exists
        // (e.g. the scheduler somehow fires twice before the consumer catches up).
        boolean alreadyPending = releaseRequestRepository
                .findByOwnerAndStatus(owner, ReleaseRequest.Status.PENDING)
                .isPresent();
        if (alreadyPending) {
            log.info("A pending release request already exists for owner {}; skipping duplicate.", owner.getEmail());
            return;
        }

        List<TrusteeLink> acceptedTrustees = trusteeLinkRepository
                .findByOwnerAndStatus(owner, TrusteeLink.TrusteeStatus.ACCEPTED);

        if (acceptedTrustees.isEmpty()) {
            log.warn("Owner {} has no accepted trustees — cannot create a release request.", owner.getEmail());
            auditService.log(owner, "ESCALATION_FAILED_NO_TRUSTEES", "No accepted trustees to notify");
            return;
        }

        // Require 2 confirmations, or all of them if fewer than 2 trustees exist.
        int required = Math.min(2, acceptedTrustees.size());

        ReleaseRequest request = ReleaseRequest.builder()
                .owner(owner)
                .requiredConfirmations(required)
                .status(ReleaseRequest.Status.PENDING)
                .build();
        request = releaseRequestRepository.save(request);

        for (TrusteeLink trusteeLink : acceptedTrustees) {
            notificationService.sendTrusteeEscalationAlert(trusteeLink.getTrusteeEmail(), owner.getEmail());
        }

        auditService.log(owner, "RELEASE_REQUEST_CREATED",
                "Release request #" + request.getId() + " created, needs " + required + " confirmations");
    }

}
