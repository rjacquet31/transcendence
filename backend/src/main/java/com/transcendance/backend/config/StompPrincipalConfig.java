package com.transcendance.backend.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class StompPrincipalConfig {

    /**
     * Attaches the Principal stored by JwtHandshakeInterceptor to the STOMP
     * session.
     *
     * Without this handler, Principal is null inside @MessageMapping methods and
     * anything sent to /user/queue/... reaches nobody.
     */
    public static class UserHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            return (Principal) attributes.get(JwtHandshakeInterceptor.PRINCIPAL_ATTRIBUTE);
        }
    }
}
