package com.transcendance.backend.undercover.entity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.undercover.entity.UndercoverClueEntity;

@Repository
public interface UndercoverClueRepository extends JpaRepository<UndercoverClueEntity, Long> {

    /**
     * The whole transcript of a match, in the order it was said.
     *
     * Participants are fetched along: every line is shown under a name.
     */
    @Query("SELECT c FROM UndercoverClueEntity c JOIN FETCH c.participant p "
            + "WHERE c.match.undercoverMatchId = :matchId "
            + "ORDER BY c.round ASC, p.seatOrder ASC")
    List<UndercoverClueEntity> findTranscript(Long matchId);

    @Query("SELECT COUNT(c) > 0 FROM UndercoverClueEntity c "
            + "WHERE c.match.undercoverMatchId = :matchId AND c.round = :round "
            + "AND c.participant.id = :participantId")
    boolean hasSpoken(Long matchId, int round, Long participantId);
}
