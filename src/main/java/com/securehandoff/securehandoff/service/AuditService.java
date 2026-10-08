package com.securehandoff.securehandoff.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.model.AuditLog;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Tamper-evident audit trail: each entry stores the hash of the previous entry,
 * so editing or deleting history breaks the chain and is detectable.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(User actor, String eventType, String description) {
        // PESSIMISTIC_WRITE on the newest row: concurrent writers queue up, so the chain cannot fork.
        String previousHash = auditLogRepository.findFirstByOrderByIdDesc()
                .map(AuditLog::getHash)
                .orElse(GENESIS_HASH);

        AuditLog entry = AuditLog.builder()
                .actor(actor)
                .eventType(eventType)
                .description(description)
                // MySQL stores microseconds; truncating here keeps the hashed value identical to the stored one.
                .occurredAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .previousHash(previousHash)
                .hash("")
                .build();

        entry.setHash(computeHash(previousHash, entry));
        auditLogRepository.save(entry); // single INSERT, never updated afterwards
    }

    @Transactional(readOnly = true)
    public List<AuditLog> history(User actor) {
        return auditLogRepository.findByActorOrderByOccurredAtDesc(actor);
    }

    /** Walks the whole chain and returns false if any entry was altered or removed. */
    @Transactional(readOnly = true)
    public boolean verifyChainIntegrity() {
        String expectedPrevious = GENESIS_HASH;
        for (AuditLog entry : auditLogRepository.findAllByOrderByIdAsc()) {
            if (!entry.getPreviousHash().equals(expectedPrevious)) {
                return false;
            }
            if (!computeHash(entry.getPreviousHash(), entry).equals(entry.getHash())) {
                return false;
            }
            expectedPrevious = entry.getHash();
        }
        return true;
    }

    private String computeHash(String previousHash, AuditLog entry) {
        try {
            String content = previousHash + "|" + entry.getActor().getId() + "|" + entry.getEventType()
                    + "|" + entry.getDescription() + "|" + entry.getOccurredAt();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
