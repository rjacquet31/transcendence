package com.transcendance.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseTokenDto {

    @NotBlank
    private String accessToken;
    
    @NotBlank
    private String refreshToken;
}