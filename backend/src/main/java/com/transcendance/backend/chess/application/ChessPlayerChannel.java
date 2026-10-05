package com.transcendance.backend.chess.application;

import java.util.UUID;

import com.transcendance.backend.chess.domain.ChessInvitation;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;

/**
 * Outbound port from the chess module towards connected players.
 *
 * Deliberately expressed in business terms: no destination, no transport type.
 * The implementation lives in the api layer and is the only place that knows
 * any of this travels over STOMP.
 */
public interface ChessPlayerChannel {

    /**
     * @return true if this player has at least one open real-time session.
     */
    boolean isOnline(UUID userId);

    /**
     * Notifies a player that he has just been invited.
     */
    void invitationSent(UUID toUserId, ChessInvitation invitation, String fromUsername);

    /**
     * Tells a player that a match has just started, so his client can open the
     * board. Carries the match itself: the invitation alone has no match id.
     */
    void matchStarted(UUID toUserId, ChessMatchEntity match);

    /**
     * Pushes a move to the player it was played against, together with the
     * resulting match state, so his board updates without polling.
     */
    void movePlayed(UUID toUserId, ChessMatchEntity match, String move);

    /**
     * Tells a player that the match is over for a reason he could not see coming
     * from the board itself: a resignation, a timeout, an opponent who never came
     * back. A game ending on a move needs no such push - the move carries the
     * final status already.
     *
     * Travels on the same destination as matchStarted: both are match lifecycle
     * events, and the status tells them apart.
     */
    void matchEnded(UUID toUserId, ChessMatchEntity match);

    /**
     * Notifies the inviter that his invitation was declined.
     */
    void invitationDeclined(UUID toUserId, ChessInvitation invitation, String fromUsername);
}
