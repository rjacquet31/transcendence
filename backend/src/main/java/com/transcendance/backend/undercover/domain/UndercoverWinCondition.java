package com.transcendance.backend.undercover.domain;


public class UndercoverWinCondition {

    /**
     * @param agents     agents still alive
     * @param undercover undercovers still alive
     * @param mrWhite    Mr Whites still alive
     * @param votesUsed  votes already spent, revotes of a same vote counting
     *                   as one
     * @param rules      the rules the match is played with
     * @return IN_PROGRESS, AGENTS_WIN or UNDERCOVER_WIN
     */
    public UndercoverMatchStatus checkWinCondition(int agents, int undercover, int mrWhite, int votesUsed,
            UndercoverRules rules) {
        int impostors = undercover + mrWhite;

        if (impostors == 0) {
            return UndercoverMatchStatus.AGENTS_WIN;
        }
        // Without parity, a table every agent has left would otherwise go on
        // forever with nobody to vote the impostors out.
        if (agents == 0) {
            return UndercoverMatchStatus.UNDERCOVER_WIN;
        }
        if (rules.isUndercoverWinOnParity() && impostors >= agents) {
            return UndercoverMatchStatus.UNDERCOVER_WIN;
        }
        if (!rules.hasVoteLeft(votesUsed)) {
            return UndercoverMatchStatus.UNDERCOVER_WIN;
        }
        return UndercoverMatchStatus.IN_PROGRESS;
    }
}
