package com.transcendance.backend.chess.api.dto;

import com.transcendance.backend.chess.persistence.entity.ChessPlayerEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChessPlayerStatDto {

    private String playerName;
    private int winCount;
    private int lossCount;
    private int drawCount;

    /**
     * Translates a player's counters into the API contract.
     *
     * The username is not carried by the chess player: it is supplied by the
     * caller, which already holds the User.
     */
    public static ChessPlayerStatDto of(ChessPlayerEntity player, String playerName) {
        return new ChessPlayerStatDto(
                playerName,
                player.getWins(),
                player.getLosses(),
                player.getDraws());
    }
}
