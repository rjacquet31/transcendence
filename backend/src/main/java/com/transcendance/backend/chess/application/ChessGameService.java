package com.transcendance.backend.chess.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.transcendance.backend.chess.domain.ChessException;
import com.transcendance.backend.chess.domain.ChessMatchStatus;
import com.transcendance.backend.chess.domain.ChessSide;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;
import com.transcendance.backend.chess.persistence.entity.ChessPlayerEntity;
import com.transcendance.backend.chess.persistence.repository.ChessMatchEntityRepository;
import com.transcendance.backend.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChessGameService {

    private final ChessMatchEntityRepository chessMatchRepository;
    private final ChessPlayerService chessPlayerService;
    private final ChessBotService chessBotService;

    /**
     * Create a match between two human players.
     *
     * The caller decides the colours: this service only records them.
     *
     * @param whiteUserId the user playing white
     * @param blackUserId the user playing black
     * @return the persisted match
     * @throws ChessException if both sides are the same user, or if
     *                        either of them is already in a match
     */
    @Transactional
    public ChessMatchEntity createNewGame(UUID whiteUserId, UUID blackUserId) {

        if (whiteUserId.equals(blackUserId)) {
            throw new ChessException.SelfPlay();
        }
        if (chessMatchRepository.existsActiveMatch(whiteUserId)) {
            throw new ChessException.AlreadyInMatch("Le joueur blanc a déjà une partie en cours");
        }
        if (chessMatchRepository.existsActiveMatch(blackUserId)) {
            throw new ChessException.AlreadyInMatch("Le joueur noir a déjà une partie en cours");
        }

        Board chessBoard = new Board();
        ChessMatchEntity chessMatch = new ChessMatchEntity(
                chessBoard,
                chessPlayerService.getOrCreatePlayer(whiteUserId),
                chessPlayerService.getOrCreatePlayer(blackUserId));

        return chessMatchRepository.save(chessMatch);
    }

    /**
     * Create a match against the bot, the human playing white.
     * The bot will play as black.
     *
     * @param userId     the user id
     * @param playerSide the side the user is playing (WHITE or BLACK)
     * @param botLevel   the level of the bot (1-10)
     * @return the persisted match
     * @throws ChessException if the user already has an active match
     */
    @Transactional
    public ChessMatchEntity createNewGameAgainstBot(UUID userId, ChessSide playerSide, int botLevel) {

        if (chessMatchRepository.existsActiveMatch(userId)) {
            throw new ChessException.AlreadyInMatch("Tu as déjà une partie en cours");
        }

        ChessPlayerEntity human = chessPlayerService.getOrCreatePlayer(userId);
        ChessMatchEntity match;
        Board chessBoard = new Board();
        if (playerSide == ChessSide.WHITE) {
            match = new ChessMatchEntity(chessBoard, human, null);
            match.setBlackBotLevel(botLevel);
        } else {
            match = new ChessMatchEntity(chessBoard, null, human);
            match.setWhiteBotLevel(botLevel);
        }
        if (botToPlay(match, chessBoard)) {
            Move moveBot = chessBotService.chooseBotMove(chessBoard, botLevel,
                    match.toSanHistory());
            applyMove(match, chessBoard, moveBot.toString());
        }
        return chessMatchRepository.save(match);
    }

    /**
     * Play a move in an existing match.
     *
     * @param matchId  the ID of the match
     * @param playerId the ID of the player making the move
     * @param move     the move in algebraic notation (e.g., "e2e4")
     * @return the updated match entity
     * @throws ChessException if the match is not found, if the player is not part
     *                        of the match,
     *                        if it's not the player's turn, or if the move is
     *                        illegal
     */
    @Transactional
    public ChessMatchEntity playMove(long matchId, UUID playerId, String move) {
        ChessMatchEntity match = chessMatchRepository.findById(matchId)
                .orElseThrow(() -> new ChessException.MatchNotFound(matchId));
        Board board = match.toBoardWithHistory();
        if (!isValidMoveFormat(move)) {
            throw new ChessException.MalformedMove(move);
        }
        if (!isPlayerInMatch(match, playerId)) {
            throw new ChessException.NotAPlayer();
        }

        if (match.getStatus() != ChessMatchStatus.IN_PROGRESS) {
            throw new ChessException.MatchAlreadyOver(match.getStatus());
        }
        if (playerId.equals(match.getWhitePlayerId())) {
            if (board.getSideToMove() != Side.WHITE) {
                throw new ChessException.NotYourTurn();
            }
        } else {
            if (board.getSideToMove() != Side.BLACK) {
                throw new ChessException.NotYourTurn();
            }
        }
        applyMove(match, board, move);
        if (botToPlay(match, board)) {
            Move moveBot = chessBotService.chooseBotMove(board, botLevelFor(match, board),
                    match.toSanHistory());
            applyMove(match, board, moveBot.toString());
        }
        chessMatchRepository.save(match);
        return match;
    }

    /**
     * Apply a move to the game.
     *
     * @param match the match entity
     * @param board the current chess board
     * @param move  the move in algebraic notation
     */
    private void applyMove(ChessMatchEntity match, Board board, String move) {
        if (!canMovePlay(board, move)) {
            throw new ChessException.IllegalMove(move);
        }
        board.doMove(new Move(move, board.getSideToMove()));
        if (endGame(board, match)) {
            recordStats(match);
            match.setEndedAt(Instant.now());
        }
        match.setFen(board.getFen());
        match.addMove(move);
    }

    /**
     * Check if a bot should play a move.
     *
     * @param match the match entity
     * @param board the current chess board
     * @return true if a bot should play, false otherwise
     */
    private boolean botToPlay(ChessMatchEntity match, Board board) {
        if ((match.getBlackBotLevel() != null || match.getWhiteBotLevel() != null)
                && match.getStatus() == ChessMatchStatus.IN_PROGRESS) {
            return board.getSideToMove() == Side.WHITE ? match.getWhiteBotLevel() != null
                    : match.getBlackBotLevel() != null;
        }
        return false;
    }

    /**
     * Check if a player is part of a match.
     *
     * @param match    the match entity
     * @param playerId the ID of the player
     * @return true if the player is part of the match, false otherwise
     */
    private boolean isPlayerInMatch(ChessMatchEntity match, UUID playerId) {
        return playerId.equals(match.getWhitePlayerId()) || playerId.equals(match.getBlackPlayerId());
    }

    /**
     * Check if a move can be played on the current board.
     *
     * @param board the current chess board
     * @param move  the move in algebraic notation
     * @return true if the move is legal, false otherwise
     */
    private boolean canMovePlay(Board board, String move) {
        Move chessMove = new Move(move, board.getSideToMove());
        return board.legalMoves().contains(chessMove);
    }

    /**
     * Check if the game has ended (checkmate, stalemate, or draw) and update the
     * match status accordingly.
     *
     * @param board the current chess board
     * @param match the match entity to update
     * @return true if the game has ended, false otherwise
     */
    private boolean endGame(Board board, ChessMatchEntity match) {
        if (board.isMated()) {
            match.setStatus(ChessMatchStatus.CHECKMATE);
            match.setWinnerSide(board.getSideToMove() == Side.WHITE ? ChessSide.BLACK : ChessSide.WHITE);
            return true;
        }
        if (board.isStaleMate()) {
            match.setStatus(ChessMatchStatus.STALEMATE);
            return true;
        }
        if(board.isRepetition()) {
            match.setStatus(ChessMatchStatus.DRAW);
            return true;
        }
        if (board.isDraw()) {
            match.setStatus(ChessMatchStatus.DRAW);
            return true;
        }
        return false;
    }

    /**
     * Validate the format of a chess move.
     *
     * @param move the move in algebraic notation
     * @return true if the move format is valid, false otherwise
     */
    public boolean isValidMoveFormat(String move) {
        return move != null && move.matches("^[a-h][1-8][a-h][1-8][qrbn]?$");
    }

    /**
     * Record the statistics of the players after a match has ended.
     *
     * @param match the match entity
     */
    private void recordStats(ChessMatchEntity match) {
        if (match.getWinnerSide() != null) {
            ChessPlayerEntity winner = match.playerOn(match.getWinnerSide());
            ChessPlayerEntity loser = match.playerOn(match.getWinnerSide().opposite());
            if (winner != null) {
                winner.incrementWins();
            }
            if (loser != null) {
                loser.incrementLosses();
            }
        } else {
            for (ChessSide side : ChessSide.values()) {
                ChessPlayerEntity player = match.playerOn(side);
                if (player != null) {
                    player.incrementDraws();
                }
            }
        }
    }

    /**
     * Starts the countdown after which a player who left forfeits his match.
     *
     * Writing the deadline to the row rather than holding a timer means a restart
     * cannot lose it: the sweep picks it up again from the database.
     *
     * @param awayPlayerId the user who just dropped his connection
     * @param grace        how long he has to come back
     * @return the id of the match now at stake, empty if he had none running
     */
    @Transactional
    public Optional<Long> armForfeit(UUID awayPlayerId, Duration grace) {
        return chessMatchRepository.findByUserId(awayPlayerId).map(match -> {
            match.armForfeit(awayPlayerId, Instant.now().plus(grace));
            return match.getChessMatchId();
        });
    }

    /**
     * Called when the away player turns out to be back before the deadline.
     */
    @Transactional
    public void cancelForfeit(long matchId) {
        chessMatchRepository.findById(matchId).ifPresent(match -> {
            if (match != null) {
                match.cancelForfeit();
            }
        });
    }

    /**
     * Resign from an ongoing match.
     *
     * @param matchId  the ID of the match
     * @param playerId the ID of the player resigning
     * @return the updated match entity
     * @throws ChessException if the match is not found, if the player is not part
     *                        of the match,
     *                        or if the match is already over
     */
    @Transactional
    public ChessMatchEntity resignMatch(long matchId, UUID playerId) {
        ChessMatchEntity match = chessMatchRepository.findById(matchId)
                .orElseThrow(() -> new ChessException.MatchNotFound(matchId));
        if (!isPlayerInMatch(match, playerId)) {
            throw new ChessException.NotAPlayer();
        }
        if (match.getStatus() != ChessMatchStatus.IN_PROGRESS) {
            throw new ChessException.MatchAlreadyOver(match.getStatus());
        }
        if (playerId.equals(match.getWhitePlayerId())) {
            match.setWinnerSide(ChessSide.BLACK);
        } else {
            match.setWinnerSide(ChessSide.WHITE);
        }
        match.setStatus(ChessMatchStatus.RESIGNED);
        recordStats(match);
        match.setEndedAt(Instant.now());
        chessMatchRepository.save(match);
        return match;
    }

    /**
     * Retrieve the active match for a given user.
     *
     * @param user the user whose active match is to be retrieved
     * @return the active ChessMatchEntity for the user
     * @throws ChessException if no active match is found for the user
     */
    @Transactional(readOnly = true)
    public ChessMatchEntity getMatchByUser(User user) {
        return chessMatchRepository.findByUserId(user.getIdUser())
                .orElseThrow(() -> new ChessException.MatchNotFoundByUserId(user.getIdUser()));
    }

    /**
     * Retrieve the matches a user has already finished, most recent first.
     *
     * A match still in progress is left out: it has no outcome to report yet, and
     * it is already served by getMatchByUser.
     *
     * @param userId the user whose history is to be retrieved
     * @return the finished matches, empty when the user has never played
     */
    @Transactional(readOnly = true)
    public List<ChessMatchEntity> getFinishedMatches(UUID userId) {
        return chessMatchRepository.findFinishedMatches(userId);
    }

    /**
     * Returns the level of the bot for the given match and board.
     *
     * @param match the match
     * @param board the board
     * @return the bot level
     */
    private int botLevelFor(ChessMatchEntity match, Board board) {
        return board.getSideToMove() == Side.WHITE
                ? match.getWhiteBotLevel()
                : match.getBlackBotLevel();
    }
}
