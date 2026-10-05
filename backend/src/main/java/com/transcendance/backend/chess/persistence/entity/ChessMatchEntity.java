package com.transcendance.backend.chess.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.transcendance.backend.chess.domain.ChessMatchStatus;
import com.transcendance.backend.chess.domain.ChessSide;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chess_matches", indexes = {
        // The forfeit sweep runs on a timer forever; without this it reads the
        // whole table every time to almost always find nothing.
        @Index(name = "idx_chess_match_forfeit_deadline", columnList = "forfeit_deadline")
})
public class ChessMatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chess_match_id")
    private Long chessMatchId;

    @NotBlank
    @Column(name = "fen", nullable = false)
    private String fen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id")
    private ChessPlayerEntity whitePlayer;

    @Column(name = "white_bot_level")
    private Integer whiteBotLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id")
    private ChessPlayerEntity blackPlayer;

    @Column(name = "black_bot_level")
    private Integer blackBotLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChessMatchStatus status = ChessMatchStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_side")
    private ChessSide winnerSide;

    private static final String MOVE_SEPARATOR = ",";

    @Column(name = "moves", columnDefinition = "text")
    private String moves = "";

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * The player who dropped his connection, if any is currently away.
     *
     * Stored as a plain column rather than a side, so a scheduled task can read
     * it off a detached match without touching the lazy player associations.
     */
    @Column(name = "away_player_id")
    private UUID awayPlayerId;

    /**
     * When the away player loses the match if he has not come back.
     *
     * Persisted rather than kept as an in-memory timer: a restart would otherwise
     * forget the countdown, and the match would stay IN_PROGRESS for good - which
     * existsActiveMatch would then read as both players being permanently busy.
     */
    @Column(name = "forfeit_deadline")
    private Instant forfeitDeadline;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public ChessMatchEntity(Board board, ChessPlayerEntity whitePlayer, ChessPlayerEntity blackPlayer) {
        this.fen = board.getFen();
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.status = ChessMatchStatus.IN_PROGRESS;
    }

    public boolean isWhiteBot() {
        return whitePlayer == null;
    }

    public boolean isBlackBot() {
        return blackPlayer == null;
    }

    public UUID getWhitePlayerId() {
        return identifierOf(whitePlayer);
    }

    public UUID getBlackPlayerId() {
        return identifierOf(blackPlayer);
    }

    /**
     * The user id on the other side of the board.
     *
     * @param userId one of the two players
     * @return the opponent's user id, or null when that side is held by a bot
     */
    /**
     * Starts the countdown after which this player forfeits.
     *
     * Overwrites any pending one: a player who drops, comes back and drops again
     * gets a fresh delay rather than the remains of the first.
     */
    public void armForfeit(UUID awayPlayerId, Instant deadline) {
        this.awayPlayerId = awayPlayerId;
        this.forfeitDeadline = deadline;
    }

    /**
     * Called once the away player is seen online again.
     */
    public void cancelForfeit() {
        this.awayPlayerId = null;
        this.forfeitDeadline = null;
    }

    public UUID opponentOf(UUID userId) {
        return userId.equals(getWhitePlayerId()) ? getBlackPlayerId() : getWhitePlayerId();
    }

    public ChessPlayerEntity playerOn(ChessSide side) {
        return side == ChessSide.WHITE ? whitePlayer : blackPlayer;
    }

    public Board toBoard() {
        Board board = new Board();
        board.loadFromFen(this.fen);
        return board;
    }

    public void addMove(String move) {
        moves = (moves == null || moves.isEmpty()) ? move : moves + MOVE_SEPARATOR + move;
    }

    public Board toBoardWithHistory() {
        Board board = new Board();

        for (String move : uciMoves()) {
            board.doMove(new Move(move, board.getSideToMove()));
        }

        return board;
    }

    public String toSanHistory() {
        MoveList played = new MoveList();
        Board board = new Board();

        for (String move : uciMoves()) {
            Move parsed = new Move(move, board.getSideToMove());
            played.add(parsed);
            board.doMove(parsed);
        }

        try {
            return played.toSan();
        } catch (MoveConversionException e) {
            throw new IllegalStateException("Unplayable move history: " + moves, e);
        }
    }

    private String[] uciMoves() {
        return (moves == null || moves.isBlank()) ? new String[0] : moves.split(MOVE_SEPARATOR);
    }

    private static UUID identifierOf(ChessPlayerEntity player) {
        return player == null ? null : player.getUserId();
    }
}
