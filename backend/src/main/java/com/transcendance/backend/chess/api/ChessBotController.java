package com.transcendance.backend.chess.api;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.chess.api.dto.ChessBotDto;
import com.transcendance.backend.chess.api.dto.ChessMatchDto;
import com.transcendance.backend.chess.application.ChessGameService;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;
import com.transcendance.backend.model.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chess/bot")
public class ChessBotController {

    private static final Logger log = LogManager.getLogger(ChessBotController.class);

    private final ChessGameService chessGameService;

    @PostMapping("/createBotMatch")
    public ResponseEntity<?> createBot(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChessBotDto botDto,
            BindingResult bindingResult) {

        if (user == null) {
            log.warn("Cannot create bot: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (bindingResult.hasErrors()) {
            String reason = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Requête invalide");
            log.warn("Cannot create bot for {}: {}", user.getUserName(), reason);
            return ResponseEntity.badRequest().body(Map.of("error", reason));
        }

        ChessMatchEntity match = chessGameService.createNewGameAgainstBot(user.getIdUser(), botDto.getSide(),
                botDto.getBotLvl());
        return ResponseEntity.ok().body(ChessMatchDto.of(match));
    }
}
