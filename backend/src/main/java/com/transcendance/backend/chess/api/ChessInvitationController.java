package com.transcendance.backend.chess.api;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.chess.api.dto.ChessInvitationDto;
import com.transcendance.backend.chess.api.dto.ChessMatchDto;
import com.transcendance.backend.chess.api.dto.ChessInviteRequestDto;
import com.transcendance.backend.chess.application.ChessInvitationService;
import com.transcendance.backend.chess.domain.ChessInvitation;
import com.transcendance.backend.chess.domain.ChessInvitationState;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;
import com.transcendance.backend.model.User;
import com.transcendance.backend.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chess/invitations")
public class ChessInvitationController {

    private static final Logger log = LogManager.getLogger(ChessInvitationController.class);

    private final ChessInvitationService chessInvitationService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> sendInvitation(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChessInviteRequestDto request,
            BindingResult bindingResult) {

        if (user == null) {
            log.warn("Chess invitation refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (bindingResult.hasErrors()) {
            String reason = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Requête invalide");
            log.warn("Chess invitation refused for {}: {}", user.getUserName(), reason);
            return ResponseEntity.badRequest().body(Map.of("error", reason));
        }

        ChessInvitation invitation = chessInvitationService.sendInvitation(
                user, request.getOpponentUsername(), request.getSide());

        return ResponseEntity.ok(
                ChessInvitationDto.of(invitation, user.getUserName(), ChessInvitationState.PENDING));

    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvitation(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChessInviteRequestDto request,
            BindingResult bindingResult) {

        if (user == null) {
            log.warn("Chess invitation acceptance refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (bindingResult.hasErrors()) {
            String reason = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Requête invalide");
            log.warn("Chess invitation acceptance refused for {}: {}", user.getUserName(), reason);
            return ResponseEntity.badRequest().body(Map.of("error", reason));
        }

        ChessMatchEntity match = chessInvitationService.acceptInvitation(
                user, request.getOpponentUsername());

        return ResponseEntity.ok(ChessMatchDto.of(match));

    }

    @PostMapping("/decline")
    public ResponseEntity<?> declineInvitation(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChessInviteRequestDto request,
            BindingResult bindingResult) {

        if (user == null) {
            log.warn("Chess invitation decline refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (bindingResult.hasErrors()) {
            String reason = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Requête invalide");
            log.warn("Chess invitation decline refused for {}: {}", user.getUserName(), reason);
            return ResponseEntity.badRequest().body(Map.of("error", reason));
        }

        ChessInvitation invitation = chessInvitationService.declineInvitation(
                user, request.getOpponentUsername());

        return ResponseEntity.ok(ChessInvitationDto.of(invitation,
                request.getOpponentUsername(), ChessInvitationState.DECLINED));

    }

    /**
     * Invitations waiting for this user, in the same shape as the ones pushed
     * over the WebSocket, so the client has a single format to parse.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingInvitations(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("Pending chess invitations refused: no authenticated user");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
    List<ChessInvitationDto> invitations = chessInvitationService.getPendingInvitations(user).stream()

        .flatMap(invitation -> userService.getUserById(invitation.fromUserId())
                .map(sender -> ChessInvitationDto.of(invitation, sender.getUserName(),
                        ChessInvitationState.PENDING))
                .stream())
        .toList();


        return ResponseEntity.ok(invitations);
    }
}
