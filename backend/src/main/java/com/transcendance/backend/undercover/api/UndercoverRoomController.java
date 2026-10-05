package com.transcendance.backend.undercover.api;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.model.User;
import com.transcendance.backend.undercover.api.dto.UndercoverRequests;
import com.transcendance.backend.undercover.application.UndercoverQueryService;
import com.transcendance.backend.undercover.application.UndercoverRoomService;
import com.transcendance.backend.undercover.domain.UndercoverException;
import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/api/undercover/rooms")
@RestController
public class UndercoverRoomController {

    private static final Logger log = LogManager.getLogger(UndercoverRoomController.class);

    private final UndercoverRoomService roomService;
    private final UndercoverQueryService queryService;
    private final UndercoverViews views;

    @PostMapping
    public ResponseEntity<?> createRoom(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("room creation");
        }
        return ResponseEntity.ok(views.room(roomService.createRoom(user.getIdUser()), user.getIdUser()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@AuthenticationPrincipal User user,
            @RequestBody UndercoverRequests.JoinRoom request) {
        if (user == null) {
            return unauthenticated("room join");
        }
        return ResponseEntity.ok(views.room(roomService.joinRoom(user.getIdUser(), request.code()), user.getIdUser()));
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leaveRoom(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("room leave");
        }
        roomService.leaveRoom(user.getIdUser());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/settings")
    public ResponseEntity<?> configureRoom(@AuthenticationPrincipal User user,
            @RequestBody UndercoverRequests.ConfigureRoom request) {
        if (user == null) {
            return unauthenticated("room settings");
        }
        UndercoverGameMode mode = request.mode();
        return ResponseEntity.ok(views.room(roomService.configure(user.getIdUser(), mode,
                mode == UndercoverGameMode.CUSTOM && request.rules() != null ? request.rules().toRules() : null),
                user.getIdUser()));
    }

    /**
     * Picks the seeded categories, or replaces the seeded pairs with the host's
     * own list.
     */
    @PutMapping("/words")
    public ResponseEntity<?> chooseWords(@AuthenticationPrincipal User user,
            @RequestBody UndercoverRequests.ChooseWords request) {
        if (user == null) {
            return unauthenticated("room words");
        }
        return ResponseEntity.ok(views.room(
                roomService.chooseWords(user.getIdUser(), request.categories(), request.wordPairs()),
                user.getIdUser()));
    }

    /**
     * The categories a room can draw its words from.
     */
    @GetMapping("/categories")
    public ResponseEntity<?> categories(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("category list");
        }
        return ResponseEntity.ok(roomService.categories());
    }

    /**
     * Starts the match. The host gets his view back; every other member hears
     * about it on the socket.
     */
    @PostMapping("/start")
    public ResponseEntity<?> startMatch(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("match start");
        }
        UndercoverMatchEntity match = roomService.startMatch(user.getIdUser());
        return ResponseEntity.ok(views.matchFor(queryService.snapshot(match.getUndercoverMatchId()),
                user.getIdUser()).orElseThrow(UndercoverException.NotAPlayer::new));
    }

    /**
     * The room this user is in. 404 when he is in none, which is the normal
     * case on arrival.
     */
    @GetMapping("/me")
    public ResponseEntity<?> myRoom(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("room lookup");
        }
        return ResponseEntity.ok(views.room(roomService.roomOf(user.getIdUser())
                .orElseThrow(UndercoverException.NotInRoom::new), user.getIdUser()));
    }

    private ResponseEntity<?> unauthenticated(String action) {
        log.warn("Undercover {} refused: no authenticated user", action);
        return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
    }
}
