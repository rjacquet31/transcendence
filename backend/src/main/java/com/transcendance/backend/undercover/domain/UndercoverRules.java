package com.transcendance.backend.undercover.domain;

import java.time.Duration;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UndercoverRules {

    public static final int MIN_PLAYERS = 3;

    /**
     * Past this, one round of descriptions takes a quarter of an hour.
     */
    public static final int MAX_PLAYERS = 20;

    public static final int DEFAULT_DESCRIPTION_SECONDS = 120;
    public static final int DEFAULT_VOTE_SECONDS = 150;
    public static final int DEFAULT_MRWHITE_VOTE_SECONDS = 90;

    private static final int MIN_PHASE_SECONDS = 15;

    @Column(name = "undercover_count", nullable = false)
    private int undercoverCount;

    @Column(name = "mr_white_count", nullable = false)
    private int mrWhiteCount;

    @Column(name = "description_rounds", nullable = false)
    private int descriptionRounds;

    @Column(name = "max_votes")
    private Integer maxVotes;

    @Column(name = "description_seconds", nullable = false)
    private int descriptionSeconds;

    @Column(name = "vote_seconds", nullable = false)
    private int voteSeconds;

    @Column(name = "mr_white_vote_seconds", nullable = false)
    private int mrWhiteVoteSeconds;

    @Column(name = "undercover_win_on_parity", nullable = false)
    private boolean undercoverWinOnParity;

    @Column(name = "tie_revotes", nullable = false)
    private int tieRevotes;

    @Column(name = "random_on_unresolved_tie", nullable = false)
    private boolean randomOnUnresolvedTie;

    /**
     * The settings a preset mode is played with.
     *
     * @throws UndercoverException.InvalidRules for CUSTOM, which has no preset
     *                                          by definition - its settings come
     *                                          from the player instead
     */
    public static UndercoverRules defaultsFor(UndercoverGameMode mode) {
        return switch (mode) {
            case CLASSIC -> UndercoverRules.builder()
                    .undercoverCount(1)
                    .mrWhiteCount(0)
                    .descriptionRounds(3)
                    .maxVotes(1)
                    .descriptionSeconds(DEFAULT_DESCRIPTION_SECONDS)
                    .voteSeconds(DEFAULT_VOTE_SECONDS)
                    .undercoverWinOnParity(false)
                    .tieRevotes(2)
                    .randomOnUnresolvedTie(false)
                    .mrWhiteVoteSeconds(DEFAULT_MRWHITE_VOTE_SECONDS)
                    .build();
            case BATTLEROYAL -> UndercoverRules.builder()
                    .undercoverCount(1)
                    .mrWhiteCount(0)
                    .descriptionRounds(1)
                    .maxVotes(null)
                    .descriptionSeconds(DEFAULT_DESCRIPTION_SECONDS)
                    .voteSeconds(DEFAULT_VOTE_SECONDS)
                    .undercoverWinOnParity(true)
                    .tieRevotes(1)
                    .randomOnUnresolvedTie(true)
                    .mrWhiteVoteSeconds(DEFAULT_MRWHITE_VOTE_SECONDS)
                    .build();
            case CUSTOM -> throw new UndercoverException.InvalidRules(
                    "Les parties personnalisées ont leurs propres règles, aucun préréglage ne s'applique");
        };
    }

    public int agentsCount(int playerCount) {
        return playerCount - impostorsCount();
    }

    public int impostorsCount() {
        return undercoverCount + mrWhiteCount;
    }

    public boolean hasMrWhite() {
        return mrWhiteCount > 0;
    }

    public boolean hasVoteLeft(int votesUsed) {
        return maxVotes == null || votesUsed < maxVotes;
    }

    /**
     * How long the given phase is allowed to last.
     *
     * @return the phase duration, or null for a phase that is not on a clock
     */
    public Duration durationOf(UndercoverPhase phase) {
        return switch (phase) {
            case DESCRIPTION -> Duration.ofSeconds(descriptionSeconds);
            case VOTE -> Duration.ofSeconds(voteSeconds);
            case MR_WHITE_GUESS -> Duration.ofSeconds(mrWhiteVoteSeconds);
            case END -> null;
        };
    }

    /**
     * Refuses settings that cannot produce a playable match.
     *
     * Called when the match is created rather than at the edge of the api, so
     * the presets are held to the same standard as what a player sends in.
     *
     * @param playerCount how many players are about to sit down
     */
    public void validateFor(int playerCount) {
        if (playerCount < MIN_PLAYERS) {
            throw new UndercoverException.InvalidRules(
                    "Une partie nécessite au moins " + MIN_PLAYERS + " joueurs (actuellement " + playerCount + ")");
        }
        if (playerCount > MAX_PLAYERS) {
            throw new UndercoverException.InvalidRules(
                    "Une partie accueille au plus " + MAX_PLAYERS + " joueurs (actuellement " + playerCount + ")");
        }
        if (undercoverCount < 1) {
            throw new UndercoverException.InvalidRules("Une partie nécessite au moins un undercover");
        }
        if (mrWhiteCount < 0) {
            throw new UndercoverException.InvalidRules("Le nombre de Mr White ne peut pas être négatif");
        }
        if (descriptionRounds < 1) {
            throw new UndercoverException.InvalidRules(
                    "Les joueurs doivent décrire au moins une fois avant l'ouverture d'un vote");
        }
        if (maxVotes != null && maxVotes < 1) {
            throw new UndercoverException.InvalidRules(
                    "Une partie sans aucun vote ne pourrait jamais être gagnée");
        }

        if (agentsCount(playerCount) < 1) {
            throw new UndercoverException.InvalidRules(
                    "Plus de place pour un agent : " + impostorsCount() + " imposteurs pour "
                            + playerCount + " joueurs");
        }

        if (tieRevotes < 0) {
            throw new UndercoverException.InvalidRules("Le nombre de votes de départage ne peut pas être négatif");
        }

        if (descriptionSeconds < MIN_PHASE_SECONDS || voteSeconds < MIN_PHASE_SECONDS
                || mrWhiteVoteSeconds < MIN_PHASE_SECONDS) {
            throw new UndercoverException.InvalidRules(
                    "Les phases doivent durer au moins " + MIN_PHASE_SECONDS + " secondes");
        }

        if (maxVotes != null && maxVotes < impostorsCount()) {
            throw new UndercoverException.InvalidRules(
                    "Les agents doivent avoir au moins un vote par imposteur : " + maxVotes + " votes pour " + impostorsCount()
                            + " imposteurs");
        }
        if (undercoverWinOnParity && agentsCount(playerCount) <= impostorsCount()) {
            throw new UndercoverException.InvalidRules(
                    "Les agents doivent être plus nombreux que les imposteurs au départ : "
                            + agentsCount(playerCount) + " contre " + impostorsCount());
        }
    }
}
