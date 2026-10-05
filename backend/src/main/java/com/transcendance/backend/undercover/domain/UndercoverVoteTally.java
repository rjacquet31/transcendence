package com.transcendance.backend.undercover.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts the ballots of one vote attempt.
 *
 * Only counts: what a tie leads to - a revote, a draw, or nobody out - depends
 * on the rules and on how many revotes were already spent, and is decided by
 * the caller.
 */
public class UndercoverVoteTally {

    public sealed interface VoteResult {

        /**
         * One seat holds strictly more votes than every other.
         */
        record Eliminated(int seat) implements VoteResult {
        }

        /**
         * Several seats share the highest count. Sorted, so two identical ties
         * always compare equal whatever order the ballots came in.
         */
        record Tie(List<Integer> seats) implements VoteResult {
        }

        /**
         * Nobody voted - every player abstained or ran out of time.
         */
        record NoVotes() implements VoteResult {
        }
    }

    /**
     * Tallies one vote attempt.
     *
     * @param targetSeats the seat each cast ballot points at, one entry per
     *                    ballot; an abstention simply adds nothing
     * @return who is out, who is tied, or that nobody voted
     */
    public VoteResult tally(List<Integer> targetSeats) {
        if (targetSeats.isEmpty()) {
            return new VoteResult.NoVotes();
        }

        Map<Integer, Integer> votesPerSeat = new HashMap<>();
        for (Integer seat : targetSeats) {
            votesPerSeat.merge(seat, 1, Integer::sum);
        }

        int highestCount = 0;
        for (int count : votesPerSeat.values()) {
            if (count > highestCount) {
                highestCount = count;
            }
        }

        List<Integer> leaders = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : votesPerSeat.entrySet()) {
            if (entry.getValue() == highestCount) {
                leaders.add(entry.getKey());
            }
        }

        if (leaders.size() == 1) {
            return new VoteResult.Eliminated(leaders.get(0));
        }
        return new VoteResult.Tie(leaders.stream().sorted().toList());
    }
}
