package com.transcendance.backend.chess.api;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.chess.api.dto.ChessMatchDto;
import com.transcendance.backend.chess.api.dto.ChessMoveRequestDto;
import com.transcendance.backend.chess.application.ChessGameService;
import com.transcendance.backend.chess.application.ChessPlayerChannel;
import com.transcendance.backend.chess.domain.ChessException;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;
import com.transcendance.backend.model.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/api/chess")
@RestController
public class ChessController {

    private static final Logger log = LogManager.getLogger(ChessController.class);

    private final ChessGameService chessGameService;
    private final ChessPlayerChannel playerChannel;

    @PostMapping("/matches/{matchId}/moves")
    public ResponseEntity<?> playMove(@AuthenticationPrincipal User user,
            @PathVariable long matchId,
            @Valid @RequestBody ChessMoveRequestDto request,
            BindingResult bindingResult) {

        if (user == null) {
            log.warn("Chess move refused on match {}: no authenticated user", matchId);
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (bindingResult.hasErrors()) {
            String reason = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Requête invalide");
            log.warn("Chess move refused for {} on match {}: {}", user.getUserName(), matchId, reason);
            return ResponseEntity.badRequest().body(Map.of("error", reason));
        }

        ChessMatchEntity match = chessGameService.playMove(
                matchId, user.getIdUser(), request.getMove());

        notifyOpponent(match, user.getIdUser(), request.getMove());

        return ResponseEntity.ok(ChessMatchDto.of(match));
    }

    /**
     * Gives the match away, without waiting for a losing position.
     *
     * Also the only way out of a match nobody is going to finish: an abandoned
     * game stays IN_PROGRESS forever, and existsActiveMatch would then keep both
     * players from ever starting another one.
     */
    @PostMapping("/matches/{matchId}/resign")
    public ResponseEntity<?> resignMatch(@AuthenticationPrincipal User user, @PathVariable long matchId) {
        if (user == null) {
            log.warn("Chess resignation refused on match {}: no authenticated user", matchId);
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        ChessMatchEntity match = chessGameService.resignMatch(matchId, user.getIdUser());

        notifyMatchEnded(match, user.getIdUser());

        return ResponseEntity.ok(ChessMatchDto.of(match));
    }

    /**
     * Retrieve the active match for the authenticated user.
     *
     * @param user the authenticated user
     * @return the active ChessMatchEntity for the user
     */
    @GetMapping("/matches/me")
    public ResponseEntity<?> getMyActiveMatch(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("Chess match retrieval refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            ChessMatchEntity match = chessGameService.getMatchByUser(user);
            return ResponseEntity.ok(ChessMatchDto.of(match));
        } catch (ChessException.MatchNotFoundByUserId e) {
            log.warn("No active chess match found for user {}: {}", user.getUserName(), e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Pushed once the move is committed, so the opponent never learns about a
     * move that a rollback would have taken back.
     *
     * Stays silent when the opponent is a bot, which has no session to push to.
     */
    private void notifyOpponent(ChessMatchEntity match, UUID moverId, String move) {
        UUID opponentId = match.opponentOf(moverId);

        if (opponentId != null) {
            playerChannel.movePlayed(opponentId, match, move);
        }
    }

    /**
     * Same timing as notifyOpponent, for an ending the opponent cannot deduce
     * from a move - here, a resignation.
     */
    private void notifyMatchEnded(ChessMatchEntity match, UUID actorId) {
        UUID opponentId = match.opponentOf(actorId);

        if (opponentId != null) {
            playerChannel.matchEnded(opponentId, match);
        }
    }
}
