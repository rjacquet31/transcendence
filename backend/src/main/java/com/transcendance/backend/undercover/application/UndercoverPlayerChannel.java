package com.transcendance.backend.undercover.application;


import com.transcendance.backend.undercover.domain.UndercoverRoom;

/**
 * Outbound port from the undercover module towards connected players.
 *
 * Expressed in business terms: the implementation in the api layer is the only
 * place that knows any of this travels over STOMP.
 *
 * Every call is made once the change is committed, so nobody hears about a
 * state a rollback would take back.
 */
public interface UndercoverPlayerChannel {

    /**
     * Sends the room as it now stands to every member.
     */
    void roomUpdated(UndercoverRoom.View room);

    /**
     * Sends every player still following the match his own view of it - his
     * word, never anyone else's role.
     */
    void matchUpdated(long matchId);

    /**
     * Broadcasts a chat line to its recipients.
     */
    void chatMessage(UndercoverChatMessage message);
}
