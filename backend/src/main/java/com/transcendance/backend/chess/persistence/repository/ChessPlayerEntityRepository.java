package com.transcendance.backend.chess.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.chess.persistence.entity.ChessPlayerEntity;

@Repository
public interface ChessPlayerEntityRepository extends JpaRepository<ChessPlayerEntity, UUID> {

}
