package com.transcendance.backend.chess.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.transcendance.backend.chess.domain.ChessSide;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChesshistoryDto {
    private long chessMatchId;
    private String fen;
    private String player1;
    private String player2;
    private String result;
    private Instant createdAt;
    private Instant endedAt;

    public static ChesshistoryDto of(ChessMatchEntity match, UUID viewerId, String player1, String player2) {
        return new ChesshistoryDto(
                match.getChessMatchId(),
                match.getFen(),
                player1,
                player2,
                resultFor(match, viewerId),
                match.getCreatedAt(),
                match.getEndedAt());
    }

    private static String resultFor(ChessMatchEntity match, UUID viewerId) {
        ChessSide winner = match.getWinnerSide();
        if (winner == null) {
            return "DRAW";
        }
        ChessSide viewerSide = viewerId.equals(match.getWhitePlayerId()) ? ChessSide.WHITE : ChessSide.BLACK;
        return winner == viewerSide ? "WIN" : "LOSS";
    }
}
