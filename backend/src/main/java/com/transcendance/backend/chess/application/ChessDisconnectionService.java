package com.transcendance.backend.chess.application;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.transcendance.backend.chess.domain.ChessException;
import com.transcendance.backend.chess.domain.ChessMatchStatus;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;
import com.transcendance.backend.chess.persistence.repository.ChessMatchEntityRepository;

import lombok.RequiredArgsConstructor;

/**
 * Gives a player who lost his connection a chance to come back, and hands the
 * match to his opponent when he does not.
 *
 * Two things count down, and they are not the same thing:
 *
 * - a timer, armed the moment the player drops, which settles his match on time
 *   and is what happens in every normal game;
 * - the deadline written on the match row, which is what that timer is really
 *   counting down to, and the only part of it that survives this process dying.
 *
 * A timer alone would leave a match IN_PROGRESS for good after a restart, and
 * existsActiveMatch would then read both players as busy forever. A deadline
 * alone would mean polling the database every few seconds for the rest of the
 * server's life. So the deadline is authoritative, the timer merely says when to
 * come and look at it, and a slow sweep picks up whatever the timers dropped.
 */
@Service
@RequiredArgsConstructor
public class ChessDisconnectionService {

    private static final Logger log = LogManager.getLogger(ChessDisconnectionService.class);

    private final ChessMatchEntityRepository chessMatchRepository;
    private final ChessGameService chessGameService;
    private final ChessPlayerChannel playerChannel;
    private final TaskScheduler taskScheduler;

    /**
     * How long a player may stay away before his match is given to his opponent.
     */
    @Value("${app.chess.disconnect-grace-seconds:60}")
    private long graceSeconds;

    /**
     * Called when a player drops his connection. If he does not come back within
     * the grace period, he will forfeit the match.
     *
     * @param userId the player who just went away
     */
    public void playerDisconnected(UUID userId) {
        Duration grace = Duration.ofSeconds(graceSeconds);

        chessGameService.armForfeit(userId, grace).ifPresent(matchId -> {
            log.info("Chess player {} dropped his connection during match {}, {}s to come back",
                    userId, matchId, graceSeconds);

            wakeUpAt(matchId, Instant.now().plus(grace));
        });
    }

    /**
     * Re-arms the countdowns that the previous process was in the middle of.
     *
     * Without this, a restart during anyone's grace period would strand his match
     * IN_PROGRESS with nobody left to end it. Deadlines already behind us fire as
     * soon as the pool takes them.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumePendingForfeits() {
        for (ChessMatchEntity match : chessMatchRepository.findArmedForfeits()) {
            log.info("Resuming forfeit countdown of chess match {}, deadline {}",
                    match.getChessMatchId(), match.getForfeitDeadline());

            wakeUpAt(match.getChessMatchId(), match.getForfeitDeadline());
        }
    }

    /**
     * Catches the matches no timer ever came back for - one that threw on an
     * unforeseen error, or was dropped by a saturated pool.
     *
     * Nothing is expected here, which is why it can afford to run this rarely: the
     * timers and the startup resume between them cover every case anyone thought
     * of, and this covers the ones nobody did.
     */
    @Scheduled(fixedRateString = "${app.chess.forfeit-sweep-ms:300000}")
    public void sweepStuckForfeits() {
        for (ChessMatchEntity match : chessMatchRepository.findExpiredForfeits(Instant.now())) {
            log.warn("Chess match {} outlived its forfeit deadline without being settled", match.getChessMatchId());

            settle(match.getChessMatchId());
        }
    }

    /**
     * Asks to be called back once this match's countdown should be over.
     */
    private void wakeUpAt(long matchId, Instant deadline) {
        taskScheduler.schedule(() -> settle(matchId), deadline);
    }

    /**
     * The verdict for one match, decided on what the row says rather than on why
     * we were woken up.
     *
     * That distinction matters: a player who drops, comes back and drops again
     * leaves a timer from his first absence still pending, due before the deadline
     * his second one wrote. Re-reading the deadline is what keeps that early timer
     * from taking the match off him ahead of time.
     *
     * Everything is caught: an exception thrown from a scheduled task is swallowed
     * by the scheduler, and one broken match must not stop the others.
     */
    private void settle(long matchId) {
        try {
            // Read outside any transaction, so only plain columns are touched here -
            // the two player associations would be unreadable on a detached match.
            ChessMatchEntity match = chessMatchRepository.findById(matchId).orElse(null);

            if (match == null || match.getStatus() != ChessMatchStatus.IN_PROGRESS) {
                return;
            }

            UUID awayUserId = match.getAwayPlayerId();
            Instant deadline = match.getForfeitDeadline();

            if (awayUserId == null || deadline == null) {
                // Someone already called the match back off.
                return;
            }

            if (Instant.now().isBefore(deadline)) {
                // A timer left over from an earlier absence; the one armed for the
                // current deadline is still to come.
                return;
            }

            if (playerChannel.isOnline(awayUserId)) {
                log.info("Chess player {} came back before the deadline, match {} goes on", awayUserId, matchId);
                chessGameService.cancelForfeit(matchId);
                return;
            }

            // No status pre-check beyond the one above: resignMatch reads it inside
            // its own transaction and refuses an already decided match itself, which
            // is the only place that can do so without racing the other player.
            ChessMatchEntity ended = chessGameService.resignMatch(matchId, awayUserId);

            log.info("Chess match {} forfeited: player {} stayed away for {}s", matchId, awayUserId, graceSeconds);

            notifyOpponent(ended, awayUserId);

        } catch (ChessException e) {
            log.info("Chess match {} needed no forfeit: {}", matchId, e.getMessage());
        } catch (Exception e) {
            log.error("Chess forfeit failed on match {}", matchId, e);
        }
    }

    /**
     * Tells the player who stayed that he has just won. Nobody else is going to:
     * the loser has no session left, and no move was played that his board could
     * have deduced the ending from.
     *
     * Stays silent when the other side is a bot, which has no session to push to.
     */
    private void notifyOpponent(ChessMatchEntity match, UUID absentUserId) {
        UUID opponentId = match.opponentOf(absentUserId);

        if (opponentId != null) {
            playerChannel.matchEnded(opponentId, match);
        }
    }
}
