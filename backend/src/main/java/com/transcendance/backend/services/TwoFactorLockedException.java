package com.transcendance.backend.services;

/**
 * Thrown when a user has sent too many wrong 2FA codes and is temporarily
 * locked out. Controllers map it to HTTP 429.
 */
public class TwoFactorLockedException extends RuntimeException {

    public TwoFactorLockedException() {
        super("Trop de tentatives, réessaie plus tard");
    }
}
