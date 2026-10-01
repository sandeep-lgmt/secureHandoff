package com.securehandoff.securehandoff.repository;

import com.securehandoff.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public class UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
