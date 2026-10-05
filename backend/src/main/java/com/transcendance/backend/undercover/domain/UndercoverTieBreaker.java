package com.transcendance.backend.undercover.domain;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import com.transcendance.backend.undercover.domain.UndercoverVoteTally.VoteResult;

public class UndercoverTieBreaker {

    public sealed interface TieOutcome {

        record Revote(List<Integer> seats) implements TieOutcome {
        }

        record Eliminated(int seat) implements TieOutcome {
        }

        record NobodyOut() implements TieOutcome {
        }
    }

    private final Random random;

    public UndercoverTieBreaker() {
        this(new SecureRandom());
    }

    /**
     * Lets a test pass a seeded Random, so a draw can be replayed exactly.
     */
    public UndercoverTieBreaker(Random random) {
        this.random = random;
    }

    /**
     * Resolves a tie.
     *
     * @param tie         the tied seats, as the tally returned them
     * @param voteAttempt 0 for the vote itself, then 1, 2... for each revote
     *                    already held - the attempt that just ended in this tie
     * @param rules       the rules the match is played with
     * @return a revote, a drawn elimination, or nobody out
     */
    public TieOutcome resolve(VoteResult.Tie tie, int voteAttempt, UndercoverRules rules) {
        if (voteAttempt < rules.getTieRevotes()) {
            return new TieOutcome.Revote(tie.seats());
        }
        if (rules.isRandomOnUnresolvedTie()) {
            List<Integer> seats = tie.seats();
            return new TieOutcome.Eliminated(seats.get(random.nextInt(seats.size())));
        }
        return new TieOutcome.NobodyOut();
    }
}
