package com.transcendance.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response of a 2FA setup: the QR code to scan (PNG as a data URI) and the
 * raw secret, for users who cannot scan and type it in by hand.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponseDto {

    private String qrCodeDataUri;

    private String secret;
}
