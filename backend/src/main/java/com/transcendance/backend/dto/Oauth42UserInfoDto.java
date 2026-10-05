package com.transcendance.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Oauth42UserInfoDto {

    /** Identifiant 42 du compte : stable, contrairement au login et a l'email. */
    private Long id;

    private String login;
    private String email;
}
