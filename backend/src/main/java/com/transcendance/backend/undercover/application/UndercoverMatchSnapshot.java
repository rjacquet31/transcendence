package com.transcendance.backend.undercover.application;

import java.util.List;
import java.util.Set;

import com.transcendance.backend.undercover.entity.UndercoverClueEntity;
import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;

/**
 * Everything a view of the match needs, read in one transaction.
 *
 * The entities are detached once handed out, but every association read by a
 * view was fetched: participants with their player, clues with their author.
 *
 * Holds secrets - every role and both words. Turning it into what one given
 * player is allowed to see is the api layer's job.
 *
 * @param votedSeats  seats that already voted in the current attempt, never
 *                    whom they voted against
 * @param tiedSeats   the seats a revote is held between; empty outside of one
 * @param guesserSeat the eliminated Mr White the match waits on, or null
 */
public record UndercoverMatchSnapshot(
        UndercoverMatchEntity match,
        List<UndercoverClueEntity> clues,
        Set<Integer> votedSeats,
        List<Integer> tiedSeats,
        Integer guesserSeat) {
}
