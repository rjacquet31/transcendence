package com.transcendance.backend.chess.api.dto;

import com.transcendance.backend.chess.domain.ChessSide;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChessInviteRequestDto {
    @NotBlank
    private String opponentUsername;

    /**
     * Colour the sender wants to play. Only read when sending an invitation;
     * optional, drawn at random when omitted.
     */
    private ChessSide side;
}
