package com.transcendance.backend.services;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.transcendance.backend.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    @Value("${app.password-reset.validity-minutes}")
    private long validityMinutes;

    public void sendPasswordResetEmail(User user, String rawToken) {
        String link = frontendOrigin.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;

        String body = """
                Bonjour %s,

                Une demande de réinitialisation du mot de passe a été faite pour votre compte.
                Cliquez sur le lien ci-dessous pour choisir un nouveau mot de passe :

                %s

                Ce lien est valable %d minutes et ne peut servir qu'une seule fois.
                Si vous n'êtes pas à l'origine de cette demande, ignorez ce message :
                votre mot de passe reste inchangé.
                """.formatted(user.getUserName(), link, validityMinutes);

        send(user.getEmail(), "Réinitialisation de votre mot de passe", body);
    }

    // Failures are logged, not thrown: the caller must answer the same way
    // whether or not the account exists.
    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send email \"{}\": {}", subject, e.getMessage());
        }
    }
}
