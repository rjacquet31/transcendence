package com.transcendance.backend.undercover.api.dto;

import com.transcendance.backend.undercover.domain.UndercoverException;
import com.transcendance.backend.undercover.domain.UndercoverRules;

/**
 * The rules of a match, both ways: shown in a room and in a match, and sent by
 * the host when he sets up a CUSTOM one.
 *
 * Boxed fields, so a missing value reads as missing rather than as zero.
 */
public record UndercoverRulesDto(
        Integer undercoverCount,
        Integer mrWhiteCount,
        Integer descriptionRounds,
        /** null for as many votes as it takes */
        Integer maxVotes,
        Integer descriptionSeconds,
        Integer voteSeconds,
        Integer mrWhiteVoteSeconds,
        Boolean undercoverWinOnParity,
        Integer tieRevotes,
        Boolean randomOnUnresolvedTie) {

    public static UndercoverRulesDto of(UndercoverRules rules) {
        return new UndercoverRulesDto(
                rules.getUndercoverCount(),
                rules.getMrWhiteCount(),
                rules.getDescriptionRounds(),
                rules.getMaxVotes(),
                rules.getDescriptionSeconds(),
                rules.getVoteSeconds(),
                rules.getMrWhiteVoteSeconds(),
                rules.isUndercoverWinOnParity(),
                rules.getTieRevotes(),
                rules.isRandomOnUnresolvedTie());
    }

    /**
     * Only checks that every field is there. Whether the rules can seat a table
     * is decided when the match starts, once the number of players is known.
     *
     * @throws UndercoverException.InvalidRules a required field is missing
     */
    public UndercoverRules toRules() {
        return UndercoverRules.builder()
                .undercoverCount(required(undercoverCount, "undercoverCount"))
                .mrWhiteCount(required(mrWhiteCount, "mrWhiteCount"))
                .descriptionRounds(required(descriptionRounds, "descriptionRounds"))
                .maxVotes(maxVotes)
                .descriptionSeconds(required(descriptionSeconds, "descriptionSeconds"))
                .voteSeconds(required(voteSeconds, "voteSeconds"))
                .mrWhiteVoteSeconds(required(mrWhiteVoteSeconds, "mrWhiteVoteSeconds"))
                .undercoverWinOnParity(required(undercoverWinOnParity, "undercoverWinOnParity"))
                .tieRevotes(required(tieRevotes, "tieRevotes"))
                .randomOnUnresolvedTie(required(randomOnUnresolvedTie, "randomOnUnresolvedTie"))
                .build();
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new UndercoverException.InvalidRules("Règle manquante : " + field);
        }
        return value;
    }
}
