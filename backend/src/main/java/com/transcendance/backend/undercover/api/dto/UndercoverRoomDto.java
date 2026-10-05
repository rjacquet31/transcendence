package com.transcendance.backend.undercover.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.transcendance.backend.model.User;
import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.domain.UndercoverRoom;
import com.transcendance.backend.undercover.domain.UndercoverRules;
import com.transcendance.backend.undercover.domain.UndercoverWordPair;

/**
 * @param categories      the seeded categories words are drawn from; empty
 *                        for every category, and unused while the host's own
 *                        list is in play
 * @param customWordCount how many pairs the host wrote; 0 when the seeded
 *                        pairs are used
 * @param customWordPairs the host's pairs themselves, for the host only: null
 *                        for every other member, who would otherwise read the
 *                        words before the match
 */
public record UndercoverRoomDto(
        String code,
        UUID hostId,
        UndercoverGameMode mode,
        UndercoverRulesDto rules,
        List<UndercoverUserDto> members,
        int minPlayers,
        int maxPlayers,
        List<String> categories,
        int customWordCount,
        List<UndercoverWordPair> customWordPairs) {

    /**
     * @param users    the members' accounts by id; a member whose account is gone
     *                 still shows, without a name
     * @param viewerId the member this copy is for
     */
    public static UndercoverRoomDto of(UndercoverRoom.View room, Map<UUID, User> users, UUID viewerId) {
        return new UndercoverRoomDto(
                room.code(),
                room.hostId(),
                room.mode(),
                UndercoverRulesDto.of(room.rules()),
                room.members().stream().map(id -> UndercoverUserDto.of(id, users.get(id))).toList(),
                UndercoverRules.MIN_PLAYERS,
                UndercoverRules.MAX_PLAYERS,
                room.categories(),
                room.customWordPairs().size(),
                room.hostId().equals(viewerId) ? room.customWordPairs() : null);
    }
}
