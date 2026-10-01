package com.securehandoff.securehandoff.repository;

import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.model.TrusteeLink;
import com.securehandoff.model.User;
import java.util.List;
import java.util.Optional;

public class TrusteeLinkRepository extends JpaRepository<TrusteeLink, Long> {
    List<TrusteeLink> findByOwner(User owner);
    List<TrusteeLink> findByOwnerAndStatus(User owner, TrusteeLink.TrusteeStatus status);
    Optional<TrusteeLink> findByInviteToken(String inviteToken);
    List<TrusteeLink> findByTrusteeUser(User trusteeUser);
}

