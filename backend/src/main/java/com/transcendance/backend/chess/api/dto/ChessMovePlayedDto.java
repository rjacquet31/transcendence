package com.transcendance.backend.chess.api.dto;

import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChessMovePlayedDto {

    private String move;
    private ChessMatchDto match;

    public static ChessMovePlayedDto of(ChessMatchEntity match, String move) {
        return new ChessMovePlayedDto(move, ChessMatchDto.of(match));
    }
}
