package com.transcendance.backend.chess.domain;

public enum ChessInvitationState {
    PENDING,
    ACCEPTED,
    DECLINED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isAccepted() {
        return this == ACCEPTED;
    }

    public boolean isDeclined() {
        return this == DECLINED;
    }
}
