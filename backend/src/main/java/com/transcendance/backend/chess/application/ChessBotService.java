package com.transcendance.backend.chess.application;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.transcendance.backend.chess.domain.BotProfile;
import com.transcendance.backend.chess.domain.ChessSearch;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChessBotService {

    private final ChessOpeningBook openingBook;
    private final Random random;

    @Autowired
    public ChessBotService(ChessOpeningBook openingBook) {
        this(openingBook, new Random());
    }

    /**
     * The move to play in this position, at this level.
     *
     * @param board      the position to move in, restored to its original state
     *                   on return
     * @param level      the bot's strength, 1 to 10
     * @param sanHistory the game so far in SAN, for the opening book; empty for
     *                   a game that has not started
     * @return a legal move, always
     * @throws IllegalStateException if the position has no legal move at all,
     *                               which means the caller let a finished game
     *                               through
     */
    public Move chooseBotMove(Board board, int level, String sanHistory) {
        BotProfile profile = BotProfile.ofLevel(level);
        List<Move> moves = board.legalMoves();

        if (moves.isEmpty()) {
            throw new IllegalStateException(
                    "Le bot n'a aucun coup à jouer : la partie est déjà terminée");
        }

        if (random.nextDouble() < profile.blunderChance()) {
            return moves.get(random.nextInt(moves.size()));
        }

        Optional<Move> fromBook = openingBook.findMove(board, sanHistory);

        if (fromBook.isPresent()) {
            return fromBook.get();
        }

        Move best = moves.get(0);
        int bestScore = -ChessSearch.INFINITY;

        for (Move move : moves) {
            board.doMove(move);
            int score = -ChessSearch.negamax(board, profile.depth() - 1,
                    -ChessSearch.INFINITY, ChessSearch.INFINITY);
            board.undoMove();

            score += noise(profile.evalNoise());

            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }

        return best;
    }

    private int noise(int amplitude) {
        return random.nextInt(2 * amplitude + 1) - amplitude;
    }
}
