package com.transcendance.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.transcendance.backend.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByOauth42Id(Long oauth42Id);

    Optional<User> findByUserName(String userName);

    boolean existsByUserName(String userName);
}
