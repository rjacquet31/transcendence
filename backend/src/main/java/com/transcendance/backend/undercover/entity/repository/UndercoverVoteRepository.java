package com.transcendance.backend.undercover.entity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;
import com.transcendance.backend.undercover.entity.UndercoverParticipantEntity;
import com.transcendance.backend.undercover.entity.UndercoverVoteEntity;

@Repository
public interface UndercoverVoteRepository extends JpaRepository<UndercoverVoteEntity, Long> {

        /**
         * The votes cast in one attempt of a round, target included.
         *
         * Filtered on the attempt: a revote must be tallied on its own, not
         * added to the ballots of the vote that ended in a tie.
         *
         * Fetched with their target: the caller tallies them, and a lazy read per
         * vote would mean a query per voter.
         */
        @Query("SELECT v FROM UndercoverVoteEntity v JOIN FETCH v.target "
                        + "WHERE v.match.undercoverMatchId = :matchId AND v.round = :round "
                        + "AND v.attempt = :attempt")
        List<UndercoverVoteEntity> findAttempt(Long matchId, int round, int attempt);

        /**
         * How many players have voted in this attempt, to tell a complete vote
         * from one still waiting on someone.
         */
        @Query("SELECT COUNT(v) FROM UndercoverVoteEntity v "
                        + "WHERE v.match.undercoverMatchId = :matchId AND v.round = :round "
                        + "AND v.attempt = :attempt")
        long countAttempt(Long matchId, int round, int attempt);

        @Query("SELECT COUNT(v) > 0 FROM UndercoverVoteEntity v "
                        + "WHERE v.match.undercoverMatchId = :matchId AND v.round = :round "
                        + "AND v.attempt = :attempt AND v.voter.id = :participantId")
        boolean hasVoted(Long matchId, int round, int attempt, Long participantId);

        /**
         * Removes every ballot of the attempt cast by or against this player.
         *
         * Used when he leaves in the middle of a vote: a ballot against someone
         * no longer at the table cannot eliminate him, and his own ballot would
         * be counted against a table he walked away from.
         */
        @Modifying(flushAutomatically = true)
        @Query("DELETE FROM UndercoverVoteEntity v WHERE v.match = :match AND v.round = :round "
                        + "AND v.attempt = :attempt AND (v.voter = :participant OR v.target = :participant)")
        int deleteBallotsInvolving(UndercoverMatchEntity match, int round, int attempt,
                        UndercoverParticipantEntity participant);
}
