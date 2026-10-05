package com.transcendance.backend.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "idUser_UNIQUE", columnNames = "idUser"),
        @UniqueConstraint(name = "email_UNIQUE", columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user")
    private UUID idUser;

    @NotBlank
    @Column(name = "user_name", nullable = false)
    private String userName;

    @NotBlank
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;

    @NotNull
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "online", nullable = false)
    private Instant online = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Renseigne uniquement sur les comptes lies a 42 ; null partout ailleurs,
     *  et Postgres tolere plusieurs null sous une contrainte unique. */
    /** False sur un compte cree par le flux 42 : son mot de passe est un UUID
     *  aleatoire jamais communique, donc le delier reviendrait a l'enfermer dehors. */
    @Column(name = "password_set", nullable = false, columnDefinition = "boolean default true")
    private boolean passwordSet = true;

    @Column(name = "oauth42_id", unique = true)
    private Long oauth42Id;

    @Column(name = "two_factor_secret")
    private String totpSecret;

    @Column(name = "two_factor_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean twoFactorEnabled = false;

    public User(String userName, String email, String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }
}
