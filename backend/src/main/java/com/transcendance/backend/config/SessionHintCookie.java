package com.transcendance.backend.config;

import org.springframework.http.ResponseCookie;

/**
 * Non-httpOnly "logged_in=1" cookie telling the frontend a session may exist.
 *
 * The real tokens are httpOnly, so the SPA cannot see them: without this hint
 * it would have to call /api/users/me on every page load and get a 401 (a red
 * line in the browser console) for anonymous visitors. It carries no secret,
 * it is only a hint: the backend never reads it.
 */
public final class SessionHintCookie {

    public static final String NAME = "logged_in";

    /** Same lifetime as the refresh token: the session can be restored until then. */
    private static final int MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private SessionHintCookie() {
    }

    public static ResponseCookie set() {
        return build("1", MAX_AGE_SECONDS);
    }

    public static ResponseCookie clear() {
        return build("", 0);
    }

    private static ResponseCookie build(String value, int maxAgeSeconds) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(false)
                .secure(true)
                .sameSite("strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
