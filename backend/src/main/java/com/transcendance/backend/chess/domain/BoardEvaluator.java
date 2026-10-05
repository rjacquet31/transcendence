package com.transcendance.backend.chess.domain;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class BoardEvaluator {

    private static final PieceType[] SCORED_TYPES = {
            PieceType.PAWN,
            PieceType.KNIGHT,
            PieceType.BISHOP,
            PieceType.ROOK,
            PieceType.QUEEN,
            PieceType.KING
    };

    /**
     * Evaluates the position from the point of view of the side to move.
     *
     * @param board the position to judge
     * @return the score in centipawns, positive when the side to move is ahead
     */
    public static int evaluate(Board board) {
        Side side = board.getSideToMove();
        boolean endgame = isEndgame(board);
        return scoreOf(board, side, endgame) - scoreOf(board, side.flip(), endgame);
    }

    /**
     * Determines if the position is in the endgame.
     *
     * @param board the position to check
     * @return true if the position is in the endgame, false otherwise
     */
    private static boolean isEndgame(Board board) {
        int material = 0;
        for (Side side : Side.values()) {
            material += Long.bitCount(board.getBitboard(Piece.make(side, PieceType.KNIGHT)))
                    * valueOf(PieceType.KNIGHT);
            material += Long.bitCount(board.getBitboard(Piece.make(side, PieceType.BISHOP)))
                    * valueOf(PieceType.BISHOP);
            material += Long.bitCount(board.getBitboard(Piece.make(side, PieceType.ROOK)))
                    * valueOf(PieceType.ROOK);
            material += Long.bitCount(board.getBitboard(Piece.make(side, PieceType.QUEEN)))
                    * valueOf(PieceType.QUEEN);
        }
        return material <= 2600;
    }

    /**
     * Returns the value of a piece type in centipawns.
     *
     * @param type the piece type
     * @return its value, 0 for the king and for no piece at all
     */
    private static int valueOf(PieceType type) {
        return switch (type) {
            case PAWN -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK -> 500;
            case QUEEN -> 900;
            default -> 0;
        };
    }

    /**
     * Evaluates the position from the point of view of a specific side.
     *
     * @param board   the position to judge
     * @param side    the side whose perspective is used
     * @param endgame whether the position is in the endgame
     * @return the score in centipawns, positive when the specified side is ahead
     */
    private static int scoreOf(Board board, Side side, boolean endgame) {
        int total = 0;

        for (PieceType type : SCORED_TYPES) {
            Piece piece = Piece.make(side, type);
            long bb = board.getBitboard(piece);

            while (bb != 0) {
                int square = Long.numberOfTrailingZeros(bb);
                total += valueOf(type) + PieceSquareTables.bonusOf(piece, square, endgame);
                bb &= bb - 1;
            }
        }

        return total;
    }

}
