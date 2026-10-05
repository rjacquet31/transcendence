package com.transcendance.backend.undercover.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A chat line checked and ready to be broadcast. Never stored.
 *
 * @param seat       the author's seat, which is how the table knows him
 * @param recipients every player still following the match, author included
 */
public record UndercoverChatMessage(long matchId, int seat, UUID authorId, String text, Instant sentAt,
        List<UUID> recipients) {
}
