package com.securehandoff.securehandoff.repository;


import com.securehandoff.model.AuditLog;
import com.securehandoff.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public class AuditlogReository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByActorOrderByOccurredAtDesc(User actor);
    Optional<AuditLog> findTopByOrderByIdDesc();
    List<AuditLog> findAllByOrderByIdAsc();
}

