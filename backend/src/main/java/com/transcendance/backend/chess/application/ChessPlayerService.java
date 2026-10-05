package com.transcendance.backend.chess.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.transcendance.backend.chess.persistence.entity.ChessPlayerEntity;
import com.transcendance.backend.chess.persistence.repository.ChessPlayerEntityRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChessPlayerService {
    
    private final ChessPlayerEntityRepository chessPlayerRepository;

    /**
     * Get or create a chess player entity for the given user ID.
     * @param userId the UUID of the user
     * @return the existing or newly created ChessPlayerEntity
     */
    @Transactional
    public ChessPlayerEntity getOrCreatePlayer(UUID userId) {
        return chessPlayerRepository.findById(userId)
                .orElseGet(() -> chessPlayerRepository.save(new ChessPlayerEntity(userId)));
    }
}
