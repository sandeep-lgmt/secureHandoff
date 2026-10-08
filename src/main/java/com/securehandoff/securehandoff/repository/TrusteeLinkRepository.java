package com.securehandoff.securehandoff.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.model.User;

public interface TrusteeLinkRepository extends JpaRepository<TrusteeLink, Long> {

    List<TrusteeLink> findByOwner(User owner);

    List<TrusteeLink> findByOwnerAndStatus(User owner, TrusteeLink.TrusteeStatus status);

    Optional<TrusteeLink> findByInviteToken(String inviteToken);

    List<TrusteeLink> findByTrusteeUser(User trusteeUser);
}
