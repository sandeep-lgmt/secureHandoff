package com.securehandoff.securehandoff.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.securehandoff.securehandoff.model.AuditLog;
import com.securehandoff.securehandoff.model.User;

import jakarta.persistence.LockModeType;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorOrderByOccurredAtDesc(User actor);

    List<AuditLog> findAllByOrderByIdAsc();

    // Locks the newest row so concurrent writers queue up and the hash chain cannot fork.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditLog> findFirstByOrderByIdDesc();
}
