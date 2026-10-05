package com.transcendance.backend.controller;

import java.security.Principal;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.transcendance.backend.services.PresenceService;

/**
 * Turns STOMP session events into the only thing presence cares about: a user
 * who has just arrived, or one whose tab has just gone.
 *
 * Sits in the api layer for the same reason ChessDisconnectListener does -
 * these are transport events, and PresenceService is kept unaware that any of
 * this travels over WebSocket.
 */
@Component
public class PresenceListener {

    private static final Logger log = LogManager.getLogger(PresenceListener.class);

    private final PresenceService presenceService;

    public PresenceListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Fired once the STOMP CONNECT frame has been accepted - not at the raw
     * WebSocket handshake. That is what makes the Principal readable here: it is
     * attached to the session by UserHandshakeHandler beforehand.
     */
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        UUID userId = userIdOf(event.getUser());

        if (userId == null) {
            return;
        }

        // SessionConnectedEvent, unlike SessionDisconnectEvent, exposes no
        // getSessionId(): the id has to be read back from the message headers.
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();

        if (sessionId == null) {
            log.warn("Ignoring connect of user {}: no session id on the CONNECT frame", userId);
            return;
        }

        presenceService.sessionOpened(userId, sessionId);
    }

    /**
     * Fired once per closed session, for every user of the application. Whether
     * it means the user is gone is for the service to decide - he may well have
     * other tabs open.
     */
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        UUID userId = userIdOf(event.getUser());

        if (userId == null) {
            return;
        }

        presenceService.sessionClosed(userId, event.getSessionId());
    }

    /**
     * Reads the user id carried by the session principal.
     *
     * @return the id, or null for a session no user can be named for
     */
    private UUID userIdOf(Principal user) {
        if (user == null || user.getName() == null) {
            return null;
        }

        try {
            // The principal name is the user id, set by JwtHandshakeInterceptor.
            return UUID.fromString(user.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring session event: principal '{}' is not a user id", user.getName());
            return null;
        }
    }
}
