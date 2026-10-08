package com.securehandoff.securehandoff.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.event.EscalationnEvent;
import com.securehandoff.securehandoff.exception.ApiException;
import com.securehandoff.securehandoff.model.CheckInConfig;
import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.CheckInConfigRepository;
import com.securehandoff.securehandoff.repository.ReleaseRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private static final String REDIS_KEY_PREFIX = "heartbeat:";

    private final CheckInConfigRepository checkInConfigRepository;
    private final ReleaseRequestRepository releaseRequestRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

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

    @Transactional
    public CheckInConfig checkIn(User owner) {
        CheckInConfig config = requireConfig(owner);

        if (config.getStatus() == CheckInConfig.CheckInStatus.RELEASED) {
            throw new ApiException("Packets were already released; check-in can no longer be reset", HttpStatus.CONFLICT);
        }

        // Owner is alive: cancel any release request that was still waiting on trustees.
        releaseRequestRepository.findFirstByOwnerAndStatus(owner, ReleaseRequest.Status.PENDING)
                .ifPresent(request -> {
                    request.setStatus(ReleaseRequest.Status.EXPIRED);
                    releaseRequestRepository.save(request);
                    auditService.log(owner, "RELEASE_REQUEST_EXPIRED",
                            "Release request #" + request.getId() + " expired because the owner checked in");
                });

        config.setLastCheckInAt(Instant.now());
        config.setStatus(CheckInConfig.CheckInStatus.ACTIVE);
        config = checkInConfigRepository.save(config);

        cacheHeartbeat(owner.getId(), config.getLastCheckInAt(), config.getFrequencyDays());
        auditService.log(owner, "CHECKED_IN", "User confirmed activity");
        return config;
    }

    @Transactional(readOnly = true)
    public CheckInConfig getStatus(User owner) {
        return requireConfig(owner);
    }

    private CheckInConfig requireConfig(User owner) {
        return checkInConfigRepository.findByOwner(owner)
                .orElseThrow(() -> new ApiException("Check-in not configured yet. Call setup first.", HttpStatus.CONFLICT));
    }

    /** Redis is only a cache; an outage must never block a check-in. */
    private void cacheHeartbeat(Long ownerId, Instant lastCheckInAt, int frequencyDays) {
        try {
            Duration ttl = Duration.ofDays(frequencyDays).plusHours(6);
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + ownerId, lastCheckInAt.toString(), ttl);
        } catch (Exception e) {
            log.warn("Could not cache heartbeat for owner {}: {}", ownerId, e.getMessage());
        }
    }

    /**
     * Hourly scan. The escalation event is published through Spring's event bus and only
     * forwarded to Kafka AFTER this transaction commits (see KafkaEventRelay).
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void scanForOverdueUsers() {
        Instant now = Instant.now();

        for (CheckInConfig config : checkInConfigRepository.findByStatusWithOwner(CheckInConfig.CheckInStatus.ACTIVE)) {
            Instant deadline = config.getLastCheckInAt().plus(config.getFrequencyDays(), ChronoUnit.DAYS);

            if (now.isAfter(deadline)) {
                config.setStatus(CheckInConfig.CheckInStatus.OVERDUE);
                checkInConfigRepository.save(config);

                User owner = config.getOwner();
                log.warn("Owner {} missed their check-in deadline. Publishing escalation event.", owner.getEmail());

                eventPublisher.publishEvent(new EscalationEvent(owner.getId(), owner.getEmail(), now));
                auditService.log(owner, "CHECKIN_OVERDUE", "Missed check-in deadline; escalation triggered");
            }
        }
    }
}
