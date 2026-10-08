package com.securehandoff.securehandoff.service;

import static com.securehandoff.securehandoff.config.KafkaConfig.RELEASE_TOPIC;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.event.ReleaseEvent;
import com.securehandoff.securehandoff.model.CheckInConfig;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.CheckInConfigRepository;
import com.securehandoff.securehandoff.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Completes the flow: once quorum is reached, mark the owner's check-in state as RELEASED. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseConsumer {

    private final UserRepository userRepository;
    private final CheckInConfigRepository checkInConfigRepository;
    private final AuditService auditService;

    @KafkaListener(topics = RELEASE_TOPIC, groupId = "securehandoff-release-group")
    @Transactional
    public void onRelease(ReleaseEvent event) {
        User owner = userRepository.findById(event.ownerId())
                .orElseThrow(() -> new IllegalStateException("Owner not found: " + event.ownerId()));

        checkInConfigRepository.findByOwner(owner).ifPresent(config -> {
            config.setStatus(CheckInConfig.CheckInStatus.RELEASED);
            checkInConfigRepository.save(config);
        });

        log.info("Release request #{} completed for owner {}", event.releaseRequestId(), owner.getEmail());
        auditService.log(owner, "PACKETS_RELEASED", "Release request #" + event.releaseRequestId() + " completed");
    }
}
