package com.securehandoff.securehandoff.repository;


import com.securehandoff.model.CheckInConfig;
import com.securehandoff.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public class CheckInConfigRepository extends JpaRepository<CheckInConfig, Long> {
    Optional<CheckInConfig> findByOwner(User owner);
    List<CheckInConfig> findByStatus(CheckInConfig.CheckInStatus status);
}
