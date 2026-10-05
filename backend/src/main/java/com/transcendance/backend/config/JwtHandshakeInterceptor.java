package com.transcendance.backend.config;

import java.security.Principal;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.transcendance.backend.services.JWTService;
import com.transcendance.backend.services.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LogManager.getLogger(JwtHandshakeInterceptor.class);

    /**
     * Key under which the Principal is stored for UserHandshakeHandler to pick up.
     */
    public static final String PRINCIPAL_ATTRIBUTE = "principal";

    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;

    public JwtHandshakeInterceptor(JWTService jwtService, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Authenticates the WebSocket handshake before it is accepted.
     *
     * The token lives in an httpOnly cookie, so JavaScript cannot read it: we read
     * it
     * here from the handshake request, the same way JWTAuthenticationFilter does
     * for REST.
     *
     * @return true if the token is valid, false to reject the connection
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket handshake refused: non-servlet request");
            return false;
        }

        String token = extractAccessTokenFromCookies(servletRequest.getServletRequest());

        if (token == null || !jwtService.isTokenValid(token)
                || !refreshTokenService.isSessionActive(jwtService.extractSessionId(token))) {
            log.warn("WebSocket handshake refused: missing or invalid access token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String userId = jwtService.extractUserId(token);
        Principal principal = () -> userId;
        attributes.put(PRINCIPAL_ATTRIBUTE, principal);

        log.info("WebSocket handshake accepted for user {}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractAccessTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
