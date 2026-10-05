package com.transcendance.backend.chess.api.dto;

import java.util.UUID;

import com.transcendance.backend.chess.domain.ChessMatchStatus;
import com.transcendance.backend.chess.domain.ChessSide;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChessMatchDto {

    private Long matchId;
    private String fen;
    private ChessSide sideToMove;
    private ChessMatchStatus status;
    private ChessSide winnerSide;
    private UUID whitePlayerId;
    private Integer whiteBotLevel;
    private UUID blackPlayerId;
    private Integer blackBotLevel;

    /**
     * Translates a match entity into the API contract.
     *
     * The side to move is not stored: it is read back from the FEN, which is the
     * single source of truth for the position.
     *
     * Must be called while the entity is still attached, since it reads the two
     * lazy player associations.
     */
    public static ChessMatchDto of(ChessMatchEntity match) {
        return new ChessMatchDto(
                match.getChessMatchId(),
                match.getFen(),
                ChessSide.from(match.toBoard().getSideToMove()),
                match.getStatus(),
                match.getWinnerSide(),
                match.getWhitePlayerId(),
                match.getWhiteBotLevel(),
                match.getBlackPlayerId(),
                match.getBlackBotLevel());
    }
}
