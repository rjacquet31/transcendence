package com.transcendance.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.dto.ForgotPasswordRequestDto;
import com.transcendance.backend.dto.ResetPasswordRequestDto;
import com.transcendance.backend.services.PasswordResetAttemptService;
import com.transcendance.backend.services.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class PasswordResetController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordResetService;
    private final PasswordResetAttemptService passwordResetAttemptService;

    // Same answer whether the email exists or not, so accounts cannot be enumerated.
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto forgotRequest,
                                            BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(fieldErrors(bindingResult));
        }

        if (!passwordResetAttemptService.tryAcquire(clientAddress(request))) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Trop de demandes de réinitialisation. Réessaie plus tard."));
        }

        passwordResetService.requestReset(forgotRequest.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "Si un compte existe avec cet email, un lien de réinitialisation vient d'être envoyé."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDto resetRequest,
                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(fieldErrors(bindingResult));
        }

        try {
            passwordResetService.resetPassword(resetRequest.getToken(),
                    resetRequest.getPassword(), resetRequest.getConfirmedPassword());
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié. Tu peux te reconnecter."));
        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    private String clientAddress(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
