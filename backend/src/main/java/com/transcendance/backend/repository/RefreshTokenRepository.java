package com.transcendance.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.model.RefreshToken;
import com.transcendance.backend.model.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    //Methode to find a refresh token by its hash
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    //Methode to drop every token issued to a user
    void deleteByUser(User user);

    //Methode to find all refresh token active from user
    List<RefreshToken> findByUserAndRevokedFalse(User user);

    boolean existsByIdTokenAndRevokedFalse(long idToken);
}