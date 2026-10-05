package com.transcendance.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.model.PasswordResetToken;
import com.transcendance.backend.model.User;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    //Methode to find a reset token by its hash
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    //Methode to find the pending reset token of a user
    Optional<PasswordResetToken> findByUser(User user);

    //Methode to drop every reset token issued to a user
    void deleteByUser(User user);

    //Methode to purge expired reset tokens
    void deleteByExpiresAtBefore(Instant now);
}
