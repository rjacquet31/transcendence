package com.transcendance.backend.undercover.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.transcendance.backend.model.User;
import com.transcendance.backend.undercover.application.UndercoverMatchSnapshot;
import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.domain.UndercoverMatchStatus;
import com.transcendance.backend.undercover.domain.UndercoverPhase;
import com.transcendance.backend.undercover.domain.UndercoverRole;
import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;
import com.transcendance.backend.undercover.entity.UndercoverParticipantEntity;

/**
 * A match as one given player is allowed to see it.
 *
 * Follows the official rules: a player gets his word but not his role - an
 * undercover does not know he is one - except Mr White, who has no word and is
 * told so. A role becomes public when its player is eliminated; every role and
 * both words once the match is over.
 *
 * Seats identify players on the wire, never participant ids.
 */
public record UndercoverMatchViewDto(
        long matchId,
        /**
         * Grows with every change of the match. Pushes can arrive out of order;
         * a client keeps the view with the highest version.
         */
        long version,
        UndercoverGameMode mode,
        UndercoverRulesDto rules,
        UndercoverMatchStatus status,
        UndercoverPhase phase,
        int currentRound,
        Integer currentSpeakerSeat,
        Instant phaseDeadline,
        /** Lets the client correct its countdown for a clock that drifts. */
        Instant serverTime,
        int votesUsed,
        int voteAttempt,
        List<Integer> votedSeats,
        List<Integer> tiedSeats,
        Integer guesserSeat,
        int mySeat,
        /** null for Mr White, who has no word */
        String myWord,
        boolean iAmMrWhite,
        boolean iAmEliminated,
        List<Player> players,
        List<Clue> clues,
        /** Both words, once the match is over; null until then. */
        String agentsWord,
        String undercoverWord) {

    public record Player(
            int seat,
            UUID userId,
            String userName,
            String avatarUrl,
            boolean eliminated,
            Integer eliminatedRound,
            boolean leftMatch,
            /** null while the role is still secret */
            UndercoverRole role) {
    }

    public record Clue(int round, int seat, String word) {
    }

    /**
     * @param viewer the participant this view is built for
     * @param users  the participants' accounts by id
     */
    public static UndercoverMatchViewDto of(UndercoverMatchSnapshot snapshot, UndercoverParticipantEntity viewer,
            Map<UUID, User> users, Instant now) {
        UndercoverMatchEntity match = snapshot.match();
        boolean over = match.getStatus().isOver();

        List<Player> players = match.getParticipants().stream()
                .map(p -> {
                    UUID userId = p.getPlayer().getUserId();
                    User user = users.get(userId);
                    boolean revealed = over || p.isEliminated();
                    return new Player(
                            p.getSeatOrder(),
                            userId,
                            user == null ? null : user.getUserName(),
                            user == null ? null : user.getAvatarUrl(),
                            p.isEliminated(),
                            p.getEliminatedRound(),
                            p.isLeftMatch(),
                            revealed ? p.getRole() : null);
                })
                .toList();

        List<Clue> clues = snapshot.clues().stream()
                .map(c -> new Clue(c.getRound(), c.getParticipant().getSeatOrder(), c.getWord()))
                .toList();

        String myWord = switch (viewer.getRole()) {
            case AGENT -> match.getAgentsWord();
            case UNDERCOVER -> match.getUndercoverWord();
            case MR_WHITE -> null;
        };

        return new UndercoverMatchViewDto(
                match.getUndercoverMatchId(),
                match.getVersion() == null ? 0 : match.getVersion(),
                match.getGameMode(),
                UndercoverRulesDto.of(match.getRules()),
                match.getStatus(),
                match.getPhase(),
                match.getCurrentRound(),
                match.getCurrentSpeakerSeat(),
                match.getPhaseDeadline(),
                now,
                match.getVotesUsed(),
                match.getVoteAttempt(),
                snapshot.votedSeats().stream().sorted().toList(),
                snapshot.tiedSeats(),
                snapshot.guesserSeat(),
                viewer.getSeatOrder(),
                myWord,
                viewer.getRole() == UndercoverRole.MR_WHITE,
                viewer.isEliminated(),
                players,
                clues,
                over ? match.getAgentsWord() : null,
                over ? match.getUndercoverWord() : null);
    }
}
