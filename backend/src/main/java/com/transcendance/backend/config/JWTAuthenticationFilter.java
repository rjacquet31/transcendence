package com.transcendance.backend.config;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transcendance.backend.repository.UserRepository;
import com.transcendance.backend.services.JWTService;
import com.transcendance.backend.services.RefreshTokenService;
import com.transcendance.backend.services.UserRoleService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;


@AllArgsConstructor
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter{

    private static final Logger log = LogManager.getLogger(JWTAuthenticationFilter.class);

    private JWTService jwtService;
    private UserRepository userRepository;
    private UserRoleService userRoleService;
    private RefreshTokenService refreshTokenService;

    /**
     * Filters the HTTP request to authenticate the user based on the JWT token.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws Exception if an error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("starting jwt filter");
        // Extract the access token from cookies
        String token = extractAccessTokenFromCookies(request);

        if (token != null) {
            try{
                if (jwtService.isTokenValid(token)
                        && refreshTokenService.isSessionActive(jwtService.extractSessionId(token))) {
                    // Extract the user ID from the token
                    String userId =jwtService.extractUserId(token);

                    // Fetch the user from the database and set the authentication
                    userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {

                        if (user.getDeletedAt() == null) {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, userRoleService.getRoleUser(user));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                });
                }
            }catch (Exception e) {
                log.warn("Invalid access token: {}", e.getMessage());
            }
        }else{
            log.info("No access token found in cookies.");
        }
        filterChain.doFilter(request, response);
    }

    private String extractAccessTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
