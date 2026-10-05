package com.transcendance.backend.chess.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.chess.api.dto.ChesshistoryDto;
import com.transcendance.backend.chess.application.ChessGameService;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chess")
public class ChessPlayerhistoryController {

    private static final Logger log = LogManager.getLogger(ChessPlayerhistoryController.class);

    /** Shown on the side of a match that was held by the engine. */
    private static final String BOT_NAME = "Bot";

    /** Shown for a player whose account is gone; the match itself remains. */
    private static final String UNKNOWN_NAME = "Unknown";

    private final ChessGameService chessGameService;
    private final UserService userService;

    /**
     * A player who has finished no match reads as an empty list rather than a
     * 404, so the client has one shape to render.
     */
    @GetMapping("/historique/me")
    public ResponseEntity<?> getMyChesshistory(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("Chess history refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        Map<UUID, String> names = new HashMap<>();
        names.put(user.getIdUser(), user.getUserName());

        List<ChesshistoryDto> history = chessGameService.getFinishedMatches(user.getIdUser()).stream()
                .map(match -> ChesshistoryDto.of(match, user.getIdUser(),
                        nameOf(names, match.getWhitePlayerId()),
                        nameOf(names, match.getBlackPlayerId())))
                .toList();

        return ResponseEntity.ok(history);
    }

    @GetMapping("/historique/{userId}")
    public ResponseEntity<?> getChessHistory(@AuthenticationPrincipal User user, @PathVariable UUID userId) {
        if (user == null) {
            log.warn("Chess history refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        Optional<User> target = userService.getUserById(userId);
        if (target.isEmpty()) {
            log.warn("Chess history refused for {}: unknown user {}", user.getUserName(), userId);
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur introuvable"));
        }

        Map<UUID, String> names = new HashMap<>();
        names.put(target.get().getIdUser(), target.get().getUserName());

        List<ChesshistoryDto> history = chessGameService.getFinishedMatches(target.get().getIdUser()).stream()
                .map(match -> ChesshistoryDto.of(match, target.get().getIdUser(),
                        nameOf(names, match.getWhitePlayerId()),
                        nameOf(names, match.getBlackPlayerId())))
                .toList();

        return ResponseEntity.ok(history);
    }

    private String nameOf(Map<UUID, String> names, UUID playerId) {
        if (playerId == null) {
            return BOT_NAME;
        }
        return names.computeIfAbsent(playerId,
                id -> userService.getUserById(id).map(User::getUserName).orElse(UNKNOWN_NAME));
    }
}
