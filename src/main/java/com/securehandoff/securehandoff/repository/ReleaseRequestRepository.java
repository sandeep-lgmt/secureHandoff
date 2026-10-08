package com.securehandoff.securehandoff.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.User;

import jakarta.persistence.LockModeType;

public interface ReleaseRequestRepository extends JpaRepository<ReleaseRequest, Long> {

    Optional<ReleaseRequest> findFirstByOwnerAndStatus(User owner, ReleaseRequest.Status status);

    List<ReleaseRequest> findByOwnerInAndStatus(Collection<User> owners, ReleaseRequest.Status status);

    // Row lock so two trustees confirming at the same moment cannot both miss the quorum.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReleaseRequest r where r.id = :id")
    Optional<ReleaseRequest> findByIdForUpdate(@Param("id") Long id);
}
