package com.transcendance.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload of a self-service profile update: username and email only, no role.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequestDto {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String userName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
}
