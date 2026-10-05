package com.transcendance.backend.services;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.transcendance.backend.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {

    // The secret key used for signing the JWT token is injected from the application properties.
    @Value("${jwt.secret}")
    private String secret;

    // Marks a token as a 42 linking state, and nothing else.
    private static final String LINK_42_PURPOSE = "link42";

    private static final String SESSION_CLAIM = "sid";

    /**
     * Generates a JWT token for the given user, tied to one session.
     *
     * @param user the user for whom the token is generated
     * @param sessionId the id of the refresh token opened alongside this token
     * @return a JWT token as a String
     */
    public String generateAccessToken(User user, long sessionId) {  

        // Create a signing key from the secret
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); 
        // Generate the JWT token with the user's ID as the subject, current time as issuedAt, and expiration time set to 15 minutes from now
        return Jwts.builder()
                .subject(String.valueOf(user.getIdUser()))
                .claim(SESSION_CLAIM, String.valueOf(sessionId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * Builds the OAuth {@code state} of an account linking round trip. The
     * session cookie is sameSite=strict, so nothing identifies the user when
     * 42 sends the browser back: this signed token does it instead. The
     * dedicated purpose claim keeps a stolen access_token from being replayed
     * here, and the five minutes cover a login on the intra, no more.
     *
     * @param user the user who asked to link their 42 account
     * @return a signed state token
     */
    public String generateLink42State(User user) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(user.getIdUser()))
                .claim("purpose", LINK_42_PURPOSE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * Reads back a state built by {@link #generateLink42State(User)}.
     *
     * @param state the state 42 handed back to the callback
     * @return the user id it carries, or null if the token is invalid,
     *         expired, or was not issued for a linking
     */
    public String extractUserIdFromLink42State(String state) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(state)
                    .getPayload();

            if (!LINK_42_PURPOSE.equals(claims.get("purpose", String.class))) {
                return null;
            }
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if the given JWT token is valid.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the user ID from the given JWT token.
     *
     * @param token the JWT token from which to extract the user ID
     * @return the user ID as a String
     */
    public String extractUserId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Extracts the session id from the given JWT token.
     *
     * @param token the JWT token from which to extract the session id
     * @return the session id, or null if the token carries none
     */
    public String extractSessionId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get(SESSION_CLAIM, String.class);
    }
}