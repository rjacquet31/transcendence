package com.transcendance.backend.chess.api;

import java.security.Principal;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.transcendance.backend.chess.application.ChessDisconnectionService;

import lombok.RequiredArgsConstructor;

/**
 * Translates a closed STOMP session into the only thing the chess module cares
 * about: a player who is no longer there.
 *
 * Lives in the api layer for the same reason ChessPlayerChannelStomp does -
 * SessionDisconnectEvent is a transport event, and the application layer is
 * kept unaware that any of this travels over WebSocket.
 */
@Component
@RequiredArgsConstructor
public class ChessDisconnectListener {

    private static final Logger log = LogManager.getLogger(ChessDisconnectListener.class);

    private final ChessDisconnectionService disconnectionService;

    /**
     * Fired once per closed session, for every user of the application - not just
     * chess players. The service decides whether it means anything.
     */
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();

        if (user == null || user.getName() == null) {
            return;
        }

        UUID userId;
        try {
            // The principal name is the user id, set by JwtHandshakeInterceptor.
            userId = UUID.fromString(user.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring disconnect of session {}: principal '{}' is not a user id",
                    event.getSessionId(), user.getName());
            return;
        }

        disconnectionService.playerDisconnected(userId);
    }
}
