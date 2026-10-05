package com.transcendance.backend.chess.domain;

import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChessSearch {

    public static final int MATE = 100_000;

    public static final int INFINITY = 1_000_000;

    public static int negamax(Board board, int depth, int alpha, int beta) {
        return negamax(board, depth, alpha, beta, new NodeCounter());
    }

    public static int negamax(Board board, int depth, int alpha, int beta, NodeCounter counter) {
        counter.visit();

        List<Move> moves = board.legalMoves();

        if (moves.isEmpty()) {
            return board.isKingAttacked() ? matedScore(depth) : 0;
        }
        if (board.isDraw()) {
            return 0;
        }
        if (depth == 0) {
            return BoardEvaluator.evaluate(board);
        }

        int best = -INFINITY;

        for (Move move : moves) {
            board.doMove(move);

            int score = -negamax(board, depth - 1, -beta, -alpha, counter);
            board.undoMove();

            if (score > best) {
                best = score;
            }
            if (best > alpha) {
                alpha = best;
            }
            if (alpha >= beta) {
                break;
            }
        }

        return best;
    }

    private static int matedScore(int depth) {
        return -MATE - depth;
    }

    public static final class NodeCounter {

        private long nodes;

        public long nodes() {
            return nodes;
        }

        void visit() {
            nodes++;
        }
    }
}
