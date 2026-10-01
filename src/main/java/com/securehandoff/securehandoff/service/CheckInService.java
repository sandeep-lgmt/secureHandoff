package com.securehandoff.securehandoff.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;

import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.repository.CheckInConfigRepository;

public class CheckInService {
     private static final String REDIS_KEY_PREFIX = "heartbeat:";

    private final CheckInConfigRepository checkInConfigRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EscalationProducer escalationProducer;
    private final AuditService auditService;
    private final ReleaseRequestRepository releaseRequestRepository;

    /** Called when the owner sets up check-ins for the first time. */
    @Transactional
    public CheckInConfig setupCheckIn(User owner, int frequencyDays) {
        CheckInConfig config = checkInConfigRepository.findByOwner(owner)
                .orElse(CheckInConfig.builder().owner(owner).build());

        config.setFrequencyDays(frequencyDays);
        config.setLastCheckInAt(Instant.now());
        config.setStatus(CheckInConfig.CheckInStatus.ACTIVE);

        config = checkInConfigRepository.save(config);
        cacheHeartbeat(owner.getId(), config.getLastCheckInAt(), frequencyDays);
        auditService.log(owner, "CHECKIN_CONFIGURED", "Check-in frequency set to every " + frequencyDays + " days");
        return config;
    }

    /** Called every time the owner "checks in" — resets their clock. */
    @Transactional
    public CheckInConfig checkIn(User owner) {
        CheckInConfig config = checkInConfigRepository.findByOwner(owner)
                .orElseThrow(() -> new ApiException("Check-in not configured yet. Call setup first.", HttpStatus.CONFLICT));

        if (config.getStatus() == CheckInConfig.CheckInStatus.RELEASED) {
            throw new ApiException("Packets were already released; check-in can no longer be reset", HttpStatus.CONFLICT);
        }

        // The owner is alive: any still-PENDING release request must die, otherwise trustees
        // could still reach quorum and decrypt the packets of someone who just checked in.
        releaseRequestRepository.findByOwnerAndStatus(owner, ReleaseRequest.Status.PENDING).ifPresent(r -> {
            r.setStatus(ReleaseRequest.Status.EXPIRED);
            releaseRequestRepository.save(r);
            auditService.log(owner, "RELEASE_REQUEST_EXPIRED", "Release request #" + r.getId() + " expired because the owner checked in");
        });

        config.setLastCheckInAt(Instant.now());
        config.setStatus(CheckInConfig.CheckInStatus.ACTIVE); // recovers from OVERDUE if they check in late but before quorum
        config = checkInConfigRepository.save(config);

        cacheHeartbeat(owner.getId(), config.getLastCheckInAt(), config.getFrequencyDays());
        auditService.log(owner, "CHECKED_IN", "User confirmed activity");
        return config;
    }

    public CheckInConfig getStatus(User owner) {
        return checkInConfigRepository.findByOwner(owner)
                .orElseThrow(() -> new ApiException("Check-in not configured yet. Call setup first.", HttpStatus.CONFLICT));
    }

    private void cacheHeartbeat(Long ownerId, Instant lastCheckInAt, int frequencyDays) {
        // TTL = frequency + a small grace window; if the key expires, that's a fast signal
        // the DB scan will confirm. Redis is the fast-path, MySQL remains the source of truth.
        long ttlSeconds = Duration.ofDays(frequencyDays).plusHours(6).getSeconds();
        redisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + ownerId, lastCheckInAt.toString(), ttlSeconds, TimeUnit.SECONDS
        );
    }

    /**
     * Runs hourly. Scans all ACTIVE check-in configs; any owner past their deadline gets
     * flipped to OVERDUE and triggers a Kafka escalation event exactly once (the status
     * flip itself is the guard against re-firing on every subsequent scan).
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // every hour
    @Transactional
    public void scanForOverdueUsers() {
        List<CheckInConfig> activeConfigs = checkInConfigRepository.findByStatusWithOwner(CheckInConfig.CheckInStatus.ACTIVE);

        for (CheckInConfig config : activeConfigs) {
            Instant deadline = config.getLastCheckInAt().plus(config.getFrequencyDays(), ChronoUnit.DAYS);

            if (Instant.now().isAfter(deadline)) {
                config.setStatus(CheckInConfig.CheckInStatus.OVERDUE);
                checkInConfigRepository.save(config);

                User owner = config.getOwner();
                log.warn("Owner {} missed check-in deadline. Firing escalation event.", owner.getEmail());

                escalationProducer.publishEscalation(
                        new EscalationEvent(owner.getId(), owner.getEmail(), Instant.now())
                );
                auditService.log(owner, "CHECKIN_OVERDUE", "Missed check-in deadline; escalation triggered");
            }
        }
    }

}
