package com.securehandoff.securehandoff.repository;
import com.securehandoff.model.ReleaseConfirmation;
import com.securehandoff.model.ReleaseRequest;
import com.securehandoff.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public class RelaseConfirmRepository extends JpaRepository<ReleaseConfirmation, Long> {
    List<ReleaseConfirmation> findByReleaseRequest(ReleaseRequest releaseRequest);
    Optional<ReleaseConfirmation> findByReleaseRequestAndTrustee(ReleaseRequest releaseRequest, User trustee);
    long countByReleaseRequest(ReleaseRequest releaseRequest);
}
