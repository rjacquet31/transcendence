package com.transcendance.backend.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.transcendance.backend.model.User;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;

/**
 * Brute-force lockout and OAuth pending logins, without a Spring context:
 * neither feature touches the database, so no UserService is needed.
 */
class TwoFactorServiceTest {

    private TwoFactorService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new TwoFactorService(null);
        user = new User();
        user.setIdUser(UUID.randomUUID());
        user.setTotpSecret(new DefaultSecretGenerator().generate());
        user.setTwoFactorEnabled(true);
    }

    private String currentCode() throws Exception {
        return new DefaultCodeGenerator().generate(user.getTotpSecret(), System.currentTimeMillis() / 1000 / 30);
    }

    private String wrongCode() throws Exception {
        return String.format("%06d", (Integer.parseInt(currentCode()) + 500000) % 1000000);
    }

    @Test
    void rightCodeIsAccepted() throws Exception {
        assertDoesNotThrow(() -> service.verifyCode(user, currentCode()));
    }

    @Test
    void locksAfterFiveWrongCodes() throws Exception {
        for (int i = 0; i < 5; i++) {
            String code = wrongCode();
            assertThrows(IllegalArgumentException.class, () -> service.verifyCode(user, code));
        }
        String code = currentCode();
        assertThrows(TwoFactorLockedException.class, () -> service.verifyCode(user, code));
    }

    @Test
    void rightCodeResetsTheCount() throws Exception {
        for (int i = 0; i < 4; i++) {
            String code = wrongCode();
            assertThrows(IllegalArgumentException.class, () -> service.verifyCode(user, code));
        }
        service.verifyCode(user, currentCode());
        for (int i = 0; i < 4; i++) {
            String code = wrongCode();
            assertThrows(IllegalArgumentException.class, () -> service.verifyCode(user, code));
        }
        assertDoesNotThrow(() -> service.verifyCode(user, currentCode()));
    }

    @Test
    void pendingLoginLifecycle() {
        String token = service.createPendingLogin(user);

        assertEquals(user.getIdUser(), service.findPendingLogin(token).orElseThrow());
        assertEquals(user.getIdUser(), service.findPendingLogin(token).orElseThrow(), "retry keeps the token");

        service.removePendingLogin(token);
        assertTrue(service.findPendingLogin(token).isEmpty());
    }

    @Test
    void unknownOrMissingTokenIsRejected() {
        service.createPendingLogin(user);
        assertTrue(service.findPendingLogin("forged-token").isEmpty());
        assertTrue(service.findPendingLogin(null).isEmpty());
    }
}
