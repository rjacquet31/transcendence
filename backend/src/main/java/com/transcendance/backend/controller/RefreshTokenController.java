package com.transcendance.backend.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.config.SessionHintCookie;
import com.transcendance.backend.dto.LoginResponseTokenDto;
import com.transcendance.backend.services.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/auth")
public class RefreshTokenController {
    
    private final RefreshTokenService refreshService;

    public RefreshTokenController(RefreshTokenService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookies(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, SessionHintCookie.clear().toString())
                    .body("Jeton de rafraîchissement introuvable");
        }
        try {
            LoginResponseTokenDto loginResponse = refreshService.refreshAccessToken(refreshToken);

            ResponseCookie accessCookie = ResponseCookie.from("access_token", loginResponse.getAccessToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("strict")
                    .path("/")
                    .maxAge(15 * 60)
                    .build();
            
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", loginResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("strict")
                    .path("/api/auth")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, SessionHintCookie.set().toString())
                    .body("Jeton rafraîchi");

        } catch (IllegalArgumentException e) {
            // Dead cookie: clear it, otherwise /login keeps rejecting with "Refresh token found"
            ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("strict")
                    .path("/api/auth")
                    .maxAge(0)
                    .build();
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                    .header(HttpHeaders.SET_COOKIE, SessionHintCookie.clear().toString())
                    .body(Map.of("error", "Jeton de rafraîchissement invalide"));
        }
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
