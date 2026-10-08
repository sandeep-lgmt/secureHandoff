package com.securehandoff.securehandoff.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securehandoff.securehandoff.model.CheckInConfig;
import com.securehandoff.securehandoff.model.User;

public interface CheckInConfigRepository extends JpaRepository<CheckInConfig, Long> {

    Optional<CheckInConfig> findByOwner(User owner);

    @Query("select c from CheckInConfig c join fetch c.owner where c.status = :status")
    List<CheckInConfig> findByStatusWithOwner(@Param("status") CheckInConfig.CheckInStatus status);
}
