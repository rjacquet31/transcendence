package com.transcendance.backend.controller;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.model.User;
import com.transcendance.backend.services.ChatService;

/**
 * REST access to the stored conversation history.
 *
 * Kept apart from ChatController, which is a STOMP @Controller: the real-time
 * and the request/response sides of the chat have nothing in common but the
 * service they call.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ChatHistoryController.class);

    private static final int MAX_LIMIT = 100;

    private final ChatService chatService;

    public ChatHistoryController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getHistory(
            @AuthenticationPrincipal User me,
            @PathVariable UUID userId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {

        if (limit < 1 || limit > MAX_LIMIT) {
            log.warn("Chat history refused: limit out of range ({})", limit);
            return ResponseEntity.badRequest().body(Map.of("error", "limit doit être compris entre 1 et " + MAX_LIMIT));
        }
        try {
            return ResponseEntity.ok(chatService.getHistory(me, userId, before, limit));
        } catch (IllegalArgumentException e) {
            log.warn("Chat history refused: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", "Impossible de lire cette conversation"));
        }
    }

    @PutMapping("/{userId}/read")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal User me,
            @PathVariable UUID userId) {

        try {
            return ResponseEntity.ok(Map.of("updated", chatService.markAsRead(me, userId)));
        } catch (IllegalArgumentException e) {
            log.warn("Mark as read refused: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", "Impossible d'accéder à cette conversation"));
        }
    }
}
