package com.transcendance.backend.chess.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


/**
 * A pending chess invitation.
 *
 * fromSide is the colour the sender chose to play; the invited user gets the
 * opposite one.
 */
public record ChessInvitation(UUID id, UUID fromUserId, UUID toUserId, ChessSide fromSide, Instant createdAt) {

    public static final Duration TTL = Duration.ofMinutes(2);

    public static ChessInvitation create(UUID fromUserId, UUID toUserId, ChessSide fromSide) {
        return new ChessInvitation(UUID.randomUUID(), fromUserId, toUserId, fromSide, Instant.now());
    }

    public boolean isExpired() {
        return createdAt.plus(TTL).isBefore(Instant.now());
    }
}
