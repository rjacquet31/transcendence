package com.transcendance.backend.undercover.domain;

public enum UndercoverMatchStatus {
    IN_PROGRESS,
    UNDERCOVER_WIN,
    AGENTS_WIN,
    MR_WHITE_WIN;

    public boolean isOver() {
        return this != IN_PROGRESS;
    }
}
