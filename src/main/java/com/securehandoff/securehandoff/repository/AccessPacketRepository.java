package com.securehandoff.securehandoff.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securehandoff.securehandoff.model.AccessPacket;
import com.securehandoff.securehandoff.model.User;

public interface AccessPacketRepository extends JpaRepository<AccessPacket, Long> {

    List<AccessPacket> findByOwner(User owner);

    Optional<AccessPacket> findByIdAndOwner(Long id, User owner);
}
