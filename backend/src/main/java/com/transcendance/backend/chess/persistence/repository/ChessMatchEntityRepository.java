package com.transcendance.backend.chess.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;

@Repository
public interface ChessMatchEntityRepository extends JpaRepository<ChessMatchEntity, Long> {

        @Query("SELECT COUNT(m) > 0 FROM ChessMatchEntity m LEFT JOIN m.whitePlayer w LEFT JOIN m.blackPlayer b "
                        + "WHERE (w.userId = :userId OR b.userId = :userId) AND m.status = IN_PROGRESS")
        boolean existsActiveMatch(UUID userId);

        @Query("SELECT m FROM ChessMatchEntity m LEFT JOIN m.whitePlayer w LEFT JOIN m.blackPlayer b "
                        + "WHERE (w.userId = :userId OR b.userId = :userId) AND m.status = IN_PROGRESS")
        Optional<ChessMatchEntity> findByUserId(UUID userId);

        /**
         * Same match as findByUserId, reduced to its id.
         *
         * Meant for callers running outside a web request - a scheduled task, a
         * WebSocket event - where the entity would come back detached and its two
         * lazy player associations could no longer be read.
         */
        @Query("SELECT m.chessMatchId FROM ChessMatchEntity m LEFT JOIN m.whitePlayer w LEFT JOIN m.blackPlayer b "
                        + "WHERE (w.userId = :userId OR b.userId = :userId) AND m.status = IN_PROGRESS")
        Optional<Long> findActiveMatchId(UUID userId);

        /**
         * Matches whose away player has run out of time to come back.
         *
         * Touches no player association, so the rows stay readable once detached -
         * this is called from a scheduled task, which has no session bound to it.
         */
        @Query("SELECT m FROM ChessMatchEntity m "
                        + "WHERE m.status = IN_PROGRESS AND m.forfeitDeadline IS NOT NULL AND m.forfeitDeadline <= :now")
        List<ChessMatchEntity> findExpiredForfeits(Instant now);

        /**
         * Every match with a countdown running, expired or not.
         *
         * Read once at startup: the timers that were counting these down died with
         * the previous process, and nothing else would ever pick them up again.
         */
        @Query("SELECT m FROM ChessMatchEntity m "
                        + "WHERE m.status = IN_PROGRESS AND m.forfeitDeadline IS NOT NULL")
        List<ChessMatchEntity> findArmedForfeits();

        /**
         * The matches this user has finished, most recent first.
         *
         * Both players are fetched with the match: the caller turns each row into
         * a name, and a lazy read per match would mean a query per match.
         *
         * The joins stay LEFT so a match against the bot, which holds no player on
         * one side, is still part of the history.
         */
        @Query("SELECT m FROM ChessMatchEntity m LEFT JOIN FETCH m.whitePlayer w LEFT JOIN FETCH m.blackPlayer b "
                        + "WHERE (w.userId = :userId OR b.userId = :userId) AND m.status <> IN_PROGRESS "
                        + "ORDER BY m.endedAt DESC")
        List<ChessMatchEntity> findFinishedMatches(UUID userId);
}
