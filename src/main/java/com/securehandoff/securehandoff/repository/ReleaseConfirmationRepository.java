package com.securehandoff.securehandoff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securehandoff.securehandoff.model.ReleaseConfirmation;
import com.securehandoff.securehandoff.model.ReleaseRequest;
import com.securehandoff.securehandoff.model.User;

public interface ReleaseConfirmationRepository extends JpaRepository<ReleaseConfirmation, Long> {

    Optional<ReleaseConfirmation> findByReleaseRequestAndTrustee(ReleaseRequest releaseRequest, User trustee);

    long countByReleaseRequest(ReleaseRequest releaseRequest);
}
