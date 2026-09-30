package com.securehandoff.securehandoff.repository;

import com.securehandoff.model.AccessPacket;
import com.securehandoff.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public class AccessPacketRepository extends JpaRepository<AccessPacket, Long> {
    List<AccessPacket> findByOwner(User owner);
    Optional<AccessPacket> findByIdAndOwner(Long id, User owner);
}