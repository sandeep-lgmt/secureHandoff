package com.securehandoff.securehandoff.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import com.securehandoff.securehandoff.model.AuditLog;


import com.securehandoff.model.AuditLog;
import com.securehandoff.model.User;
import com.securehandoff.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
     private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditLogRepository auditLogRepository;

    public void log(User actor, String eventType, String description) {
        String previousHash = auditLogRepository.findTopByOrderByIdDesc()
                .map(AuditLog::getHash)
                .orElse(GENESIS_HASH);

        AuditLog entry = AuditLog.builder()
                .actor(actor)
                .eventType(eventType)
                .description(description)
                .previousHash(previousHash)
                .hash("PENDING")
                .build();

        // occurredAt is only populated by @PrePersist on first save, and the hash needs to
        // cover it — so we save once, then compute the real hash and persist it. This row
        // is never modified again after this method returns; that's what keeps it append-only.
        entry = auditLogRepository.save(entry);
        entry.setHash(computeHash(previousHash, entry));
        auditLogRepository.save(entry);
    }

    public List<AuditLog> history(User actor) {
        return auditLogRepository.findByActorOrderByOccurredAtDesc(actor);
    }

    /** Walks the full chain and verifies no historical entry has been altered. */
    public boolean verifyChainIntegrity() {
        List<AuditLog> all = auditLogRepository.findAllByOrderByIdAsc();
        String expectedPrevious = GENESIS_HASH;

        for (AuditLog entry : all) {
            if (!entry.getPreviousHash().equals(expectedPrevious)) {
                return false;
            }
            String recomputed = computeHash(entry.getPreviousHash(), entry);
            if (!recomputed.equals(entry.getHash())) {
                return false;
            }
            expectedPrevious = entry.getHash();
        }
        return true;
    }

    private String computeHash(String previousHash, AuditLog entry) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String content = previousHash + "|" + entry.getActor().getId() + "|" + entry.getEventType()
                    + "|" + entry.getDescription() + "|" + entry.getOccurredAt();
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 should always be available on the JVM", e);
        }
    }

}
