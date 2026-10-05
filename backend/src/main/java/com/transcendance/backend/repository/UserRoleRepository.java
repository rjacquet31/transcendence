package com.transcendance.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.model.User;
import com.transcendance.backend.model.UserRole;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long>{
    
    Optional<UserRole> findByUser(User user);

    // Removes the join rows pointing at a user, which the FK forbids leaving behind
    void deleteByUser(User user);
}
