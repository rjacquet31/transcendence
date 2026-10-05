package com.transcendance.backend.chess.api;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.transcendance.backend.chess.domain.ChessException;

/**
 * Single place where a chess failure becomes an HTTP status.
 *
 * Scoped to the chess api package so it cannot change how the rest of the
 * application reports its errors.
 *
 * Keeping the mapping here rather than in catch blocks means the controllers
 * describe the happy path only, and a new ChessException subclass is wired in
 * one edit instead of one per endpoint.
 */
@RestControllerAdvice(basePackages = "com.transcendance.backend.chess.api")
public class ChessExceptionHandler {

    private static final Logger log = LogManager.getLogger(ChessExceptionHandler.class);

    /**
     * Nothing to act on: the match, the user or the invitation does not exist.
     */
    @ExceptionHandler({
            ChessException.MatchNotFound.class,
            ChessException.OpponentNotFound.class,
            ChessException.InvitationNotFound.class })
    public ResponseEntity<?> notFound(ChessException e) {
        return refuse(HttpStatus.NOT_FOUND, e);
    }

    /**
     * The target exists, but this user has no business touching it - and no
     * amount of retrying will change that.
     */
    @ExceptionHandler({
            ChessException.NotAPlayer.class,
            ChessException.NotFriends.class })
    public ResponseEntity<?> forbidden(ChessException e) {
        return refuse(HttpStatus.FORBIDDEN, e);
    }

    /**
     * The request is well formed but clashes with the current state. Unlike a
     * 403, these can succeed later: the turn comes back, a match ends, a player
     * comes online.
     */
    @ExceptionHandler({
            ChessException.MatchAlreadyOver.class,
            ChessException.NotYourTurn.class,
            ChessException.AlreadyInMatch.class,
            ChessException.OpponentOffline.class,
            ChessException.InvitationAlreadyHandled.class,
            ChessException.InvitationExpired.class,
            ChessException.DuplicateInvitation.class })
    public ResponseEntity<?> conflict(ChessException e) {
        return refuse(HttpStatus.CONFLICT, e);
    }

    /**
     * The request itself is wrong, whatever the state of the match.
     */
    @ExceptionHandler({
            ChessException.MalformedMove.class,
            ChessException.IllegalMove.class,
            ChessException.SelfPlay.class })
    public ResponseEntity<?> badRequest(ChessException e) {
        return refuse(HttpStatus.BAD_REQUEST, e);
    }

    /**
     * Two writes raced on the same match and this one lost. The position it was
     * validated against is stale, so the client has to read it again - replaying
     * the same move blindly could well be illegal now.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> concurrentUpdate(OptimisticLockingFailureException e) {
        log.warn("Chess request lost a concurrent update race: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "La partie a été modifiée entre-temps, recharge la position et réessaie"));
    }

    private ResponseEntity<?> refuse(HttpStatus status, ChessException e) {
        log.warn("Chess request refused ({}): {}", status.value(), e.getMessage());
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
