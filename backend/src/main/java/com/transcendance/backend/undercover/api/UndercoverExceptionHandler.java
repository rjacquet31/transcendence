package com.transcendance.backend.undercover.api;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.transcendance.backend.undercover.domain.UndercoverException;

/**
 * Single place where an undercover failure becomes an HTTP status.
 *
 * Scoped to the undercover api package so it cannot change how the rest of the
 * application reports its errors. Bodies take the { "error": ... } shape the
 * front already reads.
 */
@RestControllerAdvice(basePackages = "com.transcendance.backend.undercover.api")
public class UndercoverExceptionHandler {

    private static final Logger log = LogManager.getLogger(UndercoverExceptionHandler.class);

    @ExceptionHandler({
            UndercoverException.MatchNotFound.class,
            UndercoverException.RoomNotFound.class,
            UndercoverException.NotInRoom.class })
    public ResponseEntity<?> notFound(UndercoverException e) {
        return refuse(HttpStatus.NOT_FOUND, e);
    }

    /**
     * The target exists, but this user has no business doing that - and no
     * amount of retrying will change it.
     */
    @ExceptionHandler({
            UndercoverException.NotAPlayer.class,
            UndercoverException.PlayerEliminated.class,
            UndercoverException.NotTheGuesser.class,
            UndercoverException.NotRoomHost.class })
    public ResponseEntity<?> forbidden(UndercoverException e) {
        return refuse(HttpStatus.FORBIDDEN, e);
    }

    /**
     * Clashes with the current state, and may succeed later: the turn comes
     * back, a phase opens, a player frees himself.
     */
    @ExceptionHandler({
            UndercoverException.MatchAlreadyOver.class,
            UndercoverException.WrongPhase.class,
            UndercoverException.NotYourTurn.class,
            UndercoverException.AlreadyVoted.class,
            UndercoverException.AlreadyInMatch.class,
            UndercoverException.RoomFull.class,
            UndercoverException.NotEnoughPlayers.class })
    public ResponseEntity<?> conflict(UndercoverException e) {
        return refuse(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler({
            UndercoverException.InvalidRules.class,
            UndercoverException.InvalidWordList.class,
            UndercoverException.DuplicatePlayer.class,
            UndercoverException.InvalidClue.class,
            UndercoverException.InvalidVoteTarget.class,
            UndercoverException.InvalidGuess.class,
            UndercoverException.InvalidChatMessage.class })
    public ResponseEntity<?> badRequest(UndercoverException e) {
        return refuse(HttpStatus.BAD_REQUEST, e);
    }

    /**
     * Seeding problem: the server cannot start any match until it is fixed.
     */
    @ExceptionHandler(UndercoverException.NoWordPair.class)
    public ResponseEntity<?> unavailable(UndercoverException e) {
        log.error("Undercover match refused: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<?> busy(PessimisticLockingFailureException e) {
        log.warn("Undercover request could not lock its match: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "La partie est occupée, réessaie"));
    }

    private ResponseEntity<?> refuse(HttpStatus status, UndercoverException e) {
        log.warn("Undercover request refused ({}): {}", status.value(), e.getMessage());
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
