package com.transcendance.backend.chess.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;

@Component
public class ChessOpeningBook {

    private static final String BOOK_RESOURCE = "/games.txt";

    private static final int MAX_BOOK_PLIES = 10;

    private static final String COMMENT_PREFIX = "#";

    private static final List<String> RESULTS = List.of("1-0", "0-1", "1/2-1/2", "*");

    private final List<String> games;
    private final Random random;

    public ChessOpeningBook() {
        this(new Random());
    }

    ChessOpeningBook(Random random) {
        this.random = random;
        this.games = loadGames();
    }

    public Optional<Move> findMove(Board board, String sanHistory) {
        String prefix = sanHistory == null ? "" : sanHistory;

        if (plyCount(prefix) >= MAX_BOOK_PLIES) {
            return Optional.empty();
        }

        Map<String, Integer> continuations = continuationsAfter(prefix);

        if (continuations.isEmpty()) {
            return Optional.empty();
        }

        return decode(board, drawWeighted(continuations))
                .filter(move -> board.legalMoves().contains(move));
    }

    private Map<String, Integer> continuationsAfter(String prefix) {
        Map<String, Integer> continuations = new LinkedHashMap<>();

        for (String game : games) {
            if (!game.startsWith(prefix)) {
                continue;
            }

            String next = firstToken(game.substring(prefix.length()));

            if (next != null && !RESULTS.contains(next)) {
                continuations.merge(next, 1, Integer::sum);
            }
        }

        return continuations;
    }

    private String drawWeighted(Map<String, Integer> continuations) {
        int total = continuations.values().stream().mapToInt(Integer::intValue).sum();
        int draw = random.nextInt(total);

        for (Map.Entry<String, Integer> continuation : continuations.entrySet()) {
            draw -= continuation.getValue();

            if (draw < 0) {
                return continuation.getKey();
            }
        }

        throw new IllegalStateException("Weighted draw fell through " + total + " games");
    }

    private Optional<Move> decode(Board board, String san) {
        try {
            MoveList decoded = new MoveList(board.getFen());
            decoded.addSanMove(san, true, true);

            return Optional.of(decoded.getFirst());
        } catch (MoveConversionException e) {
            return Optional.empty();
        }
    }

    private static int plyCount(String sanHistory) {
        return sanHistory.isBlank() ? 0 : sanHistory.trim().split("\\s+").length;
    }

    private static String firstToken(String text) {
        String trimmed = text.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        int space = trimmed.indexOf(' ');

        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private static List<String> loadGames() {
        try (InputStream resource = ChessOpeningBook.class.getResourceAsStream(BOOK_RESOURCE)) {
            if (resource == null) {
                throw new IllegalStateException(
                        "Opening book " + BOOK_RESOURCE + " is not on the classpath");
            }

            List<String> games = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource, StandardCharsets.UTF_8))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(COMMENT_PREFIX) && !line.isBlank()) {
                        games.add(line.trim());
                    }
                }
            }

            return List.copyOf(games);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read opening book " + BOOK_RESOURCE, e);
        }
    }

    public int size() {
        return games.size();
    }
}
