package com.transcendance.backend.chess.api;

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

import com.transcendance.backend.chess.api.dto.ChessPlayerStatDto;
import com.transcendance.backend.chess.application.ChessPlayerService;
import com.transcendance.backend.chess.persistence.entity.ChessPlayerEntity;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/api/chess")
@RestController
public class ChessPlayerStatController {

    private static final Logger log = LogManager.getLogger(ChessPlayerStatController.class);

    private final ChessPlayerService chessPlayerService;
    private final UserService userService;

    @GetMapping("/stats/me")
    public ResponseEntity<?> getChessPlayerStat(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("Chess stats refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        ChessPlayerEntity player = chessPlayerService.getOrCreatePlayer(user.getIdUser());

        return ResponseEntity.ok(ChessPlayerStatDto.of(player, user.getUserName()));
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getChessPlayerStat(@AuthenticationPrincipal User user, @PathVariable UUID userId) {
        if (user == null) {
            log.warn("Chess stats refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        Optional<User> target = userService.getUserById(userId);
        if (target.isEmpty()) {
            log.warn("Chess stats refused for {}: unknown user {}", user.getUserName(), userId);
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur introuvable"));
        }

        ChessPlayerEntity player = chessPlayerService.getOrCreatePlayer(userId);

        return ResponseEntity.ok(ChessPlayerStatDto.of(player, target.get().getUserName()));
    }
}
