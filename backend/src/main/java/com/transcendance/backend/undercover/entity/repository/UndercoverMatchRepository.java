package com.transcendance.backend.undercover.entity.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface UndercoverMatchRepository extends JpaRepository<UndercoverMatchEntity, Long> {

    /**
     * Whether this user still sits at a match in progress.
     *
     * A player who left the table no longer counts, eliminated or not: leaving
     * is precisely how he gets free to play another match.
     */
    @Query("SELECT COUNT(m) > 0 FROM UndercoverMatchEntity m JOIN m.participants p "
            + "WHERE p.player.userId = :userId AND p.leftMatch = false AND m.status = IN_PROGRESS")
    boolean existsActiveMatch(UUID userId);

    /**
     * The id of the match this user still sits at, if any.
     *
     * At most one, since existsActiveMatch keeps anyone from being seated twice.
     */
    @Query("SELECT m.undercoverMatchId FROM UndercoverMatchEntity m JOIN m.participants p "
            + "WHERE p.player.userId = :userId AND p.leftMatch = false AND m.status = IN_PROGRESS")
    Optional<Long> findActiveMatchId(UUID userId);

    /**
     * The match with its participants and their players already loaded.
     *
     * Every turn of play reads the whole table - who speaks next, who is still
     * alive, who voted - so the alternative is a query per participant.
     */
    @Query("SELECT m FROM UndercoverMatchEntity m "
            + "LEFT JOIN FETCH m.participants p LEFT JOIN FETCH p.player "
            + "WHERE m.undercoverMatchId = :matchId")
    Optional<UndercoverMatchEntity> findByIdWithParticipants(Long matchId);

    /**
     * The match, with its row locked until the transaction ends.
     *
     * Every action that moves a match along goes through this: two players
     * sending the last two votes at the same instant would otherwise each count
     * the ballots without seeing the other's, and neither would close the vote.
     *
     * No join fetch on purpose: Postgres refuses FOR UPDATE on the nullable
     * side of an outer join. Participants load lazily, inside the same
     * transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM UndercoverMatchEntity m WHERE m.undercoverMatchId = :matchId")
    Optional<UndercoverMatchEntity> findByIdForUpdate(Long matchId);

    /**
     * Matches whose current phase has run out of time, as ids.
     *
     * Only ids: each one is then locked and checked again in its own
     * transaction, since a player may have acted between this read and then.
     */
    @Query("SELECT m.undercoverMatchId FROM UndercoverMatchEntity m "
            + "WHERE m.status = IN_PROGRESS AND m.phaseDeadline IS NOT NULL AND m.phaseDeadline <= :now")
    List<Long> findExpiredPhaseIds(Instant now);

    /**
     * The matches this user has finished, most recent first.
     */
    @Query("SELECT DISTINCT m FROM UndercoverMatchEntity m "
            + "LEFT JOIN FETCH m.participants p LEFT JOIN FETCH p.player "
            + "WHERE EXISTS (SELECT 1 FROM UndercoverParticipantEntity q "
            + "WHERE q.match = m AND q.player.userId = :userId) "
            + "AND m.status <> IN_PROGRESS ORDER BY m.endedAt DESC")
    List<UndercoverMatchEntity> findFinishedMatches(UUID userId);
}
