package com.transcendance.backend.chess.domain;

public enum ChessMatchStatus {
    IN_PROGRESS,
    CHECKMATE,
    STALEMATE,
    DRAW,
    RESIGNED;

    public boolean isOver() {
        return this != IN_PROGRESS;
    }
}
