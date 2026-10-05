package com.transcendance.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Method to find a role by its name
    Optional<Role> findByRoleName(String roleName);
}
