package com.transcendance.backend.undercover.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.transcendance.backend.undercover.domain.UndercoverException;
import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.domain.UndercoverRoom;
import com.transcendance.backend.undercover.domain.UndercoverRules;
import com.transcendance.backend.undercover.domain.UndercoverWordMatcher;
import com.transcendance.backend.undercover.domain.UndercoverWordPair;
import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;
import com.transcendance.backend.undercover.entity.repository.UndercoverMatchRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverWordPairRepository;

import lombok.RequiredArgsConstructor;

/**
 * Waiting rooms, joined with a short code, where players gather before a match.
 *
 * Kept in memory, like chess invitations: a restart only loses rooms nobody has
 * started yet. Every method holds the service's lock - rooms are few and their
 * operations quick, and one lock rules out a player ending up in two rooms.
 */
@Service
@RequiredArgsConstructor
public class UndercoverRoomService {

    private static final Logger log = LogManager.getLogger(UndercoverRoomService.class);

    /** No 0/O or 1/I: codes are read aloud and typed by hand. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    /** A room nobody touched for this long is dropped. */
    private static final Duration IDLE_TIMEOUT = Duration.ofHours(2);

    private final UndercoverService undercoverService;
    private final UndercoverMatchRepository matchRepository;
    private final UndercoverWordPairRepository wordPairRepository;
    private final UndercoverWordMatcher wordMatcher;
    private final UndercoverPlayerChannel playerChannel;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, UndercoverRoom> roomsByCode = new HashMap<>();
    private final Map<UUID, String> roomCodeByUser = new HashMap<>();

    /**
     * Opens a room with this user as host, leaving the room he was in if any.
     *
     * @throws UndercoverException.AlreadyInMatch the user still sits at a match
     */
    public synchronized UndercoverRoom.View createRoom(UUID userId) {
        requireNotInMatch(userId);
        leaveCurrentRoom(userId);

        UndercoverRoom room = new UndercoverRoom(newCode(), userId, clock.instant());
        roomsByCode.put(room.getCode(), room);
        roomCodeByUser.put(userId, room.getCode());
        return room.view();
    }

    /**
     * Joins the room with this code, leaving the room he was in if any.
     *
     * @throws UndercoverException.RoomNotFound   no room with this code
     * @throws UndercoverException.RoomFull       the room seats no one more
     * @throws UndercoverException.AlreadyInMatch the user still sits at a match
     */
    public synchronized UndercoverRoom.View joinRoom(UUID userId, String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        UndercoverRoom room = roomsByCode.get(normalized);
        if (room == null) {
            throw new UndercoverException.RoomNotFound(normalized);
        }
        if (room.getMembers().contains(userId)) {
            return room.view();
        }
        requireNotInMatch(userId);
        if (room.size() >= UndercoverRules.MAX_PLAYERS) {
            throw new UndercoverException.RoomFull(UndercoverRules.MAX_PLAYERS);
        }

        leaveCurrentRoom(userId);
        room.add(userId, clock.instant());
        roomCodeByUser.put(userId, room.getCode());

        UndercoverRoom.View view = room.view();
        playerChannel.roomUpdated(view);
        return view;
    }

    /**
     * Leaves the current room. The next member becomes host if needed; an
     * emptied room is closed. Doing it outside of any room changes nothing.
     */
    public synchronized void leaveRoom(UUID userId) {
        leaveCurrentRoom(userId);
    }

    /**
     * @throws UndercoverException.NotInRoom   the user is in no room
     * @throws UndercoverException.NotRoomHost the user is not the host
     * @throws UndercoverException.InvalidRules CUSTOM without rules
     */
    public synchronized UndercoverRoom.View configure(UUID userId, UndercoverGameMode mode,
            UndercoverRules customRules) {
        UndercoverRoom room = hostedRoom(userId);
        if (mode == null) {
            throw new UndercoverException.InvalidRules("Un mode est obligatoire");
        }
        if (mode == UndercoverGameMode.CUSTOM && customRules == null) {
            throw new UndercoverException.InvalidRules("Une partie personnalisée doit avoir ses règles");
        }
        room.configure(mode, customRules, clock.instant());

        UndercoverRoom.View view = room.view();
        playerChannel.roomUpdated(view);
        return view;
    }

    /**
     * Picks what the words are drawn from: some seeded categories, or the
     * host's own list, which then replaces the seeded pairs altogether.
     *
     * @param categories      null or empty for every category; kept while
     *                        custom pairs are used, so going back to the seeded
     *                        pairs finds them as they were
     * @param customWordPairs null or empty to play on the seeded pairs
     * @throws UndercoverException.NotInRoom       the user is in no room
     * @throws UndercoverException.NotRoomHost     the user is not the host
     * @throws UndercoverException.InvalidWordList an unknown category, or a
     *                                             list that cannot be played
     */
    public synchronized UndercoverRoom.View chooseWords(UUID userId, List<String> categories,
            List<UndercoverWordPair> customWordPairs) {
        UndercoverRoom room = hostedRoom(userId);

        List<UndercoverWordPair> pairs = cleanWordPairs(customWordPairs);
        List<String> chosen = cleanCategories(categories);
        room.chooseWords(chosen, pairs, clock.instant());

        UndercoverRoom.View view = room.view();
        playerChannel.roomUpdated(view);
        return view;
    }

    /**
     * @return every category a room can pick, sorted
     */
    public List<String> categories() {
        return wordPairRepository.findCategories();
    }

    /**
     * Drops blanks and repeats, and refuses a category nothing is seeded in.
     */
    private List<String> cleanCategories(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        List<String> known = wordPairRepository.findCategories();
        List<String> chosen = categories.stream()
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .toList();
        for (String category : chosen) {
            if (!known.contains(category)) {
                throw new UndercoverException.InvalidWordList("Catégorie inconnue : " + category);
            }
        }
        return chosen;
    }

    /**
     * Trims every word and refuses the pairs a match could not be played on.
     */
    private List<UndercoverWordPair> cleanWordPairs(List<UndercoverWordPair> pairs) {
        if (pairs == null) {
            return List.of();
        }
        if (pairs.size() > UndercoverWordPair.MAX_CUSTOM_PAIRS) {
            throw new UndercoverException.InvalidWordList(
                    "Une liste de mots contient au plus " + UndercoverWordPair.MAX_CUSTOM_PAIRS + " paires");
        }

        List<UndercoverWordPair> cleaned = new ArrayList<>(pairs.size());
        for (UndercoverWordPair pair : pairs) {
            if (pair == null) {
                throw new UndercoverException.InvalidWordList("Une paire est manquante");
            }
            String agentsWord = cleanWord(pair.agentsWord());
            String undercoverWord = cleanWord(pair.undercoverWord());
            // Compared the way clues are: two words that only differ by case
            // or accents would hand everyone the same word.
            if (wordMatcher.normalize(agentsWord).equals(wordMatcher.normalize(undercoverWord))) {
                throw new UndercoverException.InvalidWordList(
                        "Les deux mots d'une paire doivent être différents : " + agentsWord);
            }
            cleaned.add(new UndercoverWordPair(agentsWord, undercoverWord));
        }
        return cleaned;
    }

    private String cleanWord(String word) {
        String trimmed = word == null ? "" : word.trim();
        // Normalized, since a word made of punctuation alone could never be
        // said nor guessed.
        if (wordMatcher.normalize(trimmed).isEmpty()) {
            throw new UndercoverException.InvalidWordList("Chaque paire doit contenir deux mots");
        }
        if (trimmed.length() > UndercoverWordPair.MAX_WORD_LENGTH) {
            throw new UndercoverException.InvalidWordList(
                    "Un mot fait au plus " + UndercoverWordPair.MAX_WORD_LENGTH + " caractères");
        }
        return trimmed;
    }

    /**
     * Starts the match with every member of the room. The room stays open
     * behind it: its members come back to it once the match is over, and the
     * host may start another one without sharing the code again.
     *
     * A refused start - rules that do not fit the table, a member who joined a
     * match meanwhile, a match of this room still being played - leaves the
     * room as it was.
     *
     * @throws UndercoverException.NotInRoom        the user is in no room
     * @throws UndercoverException.NotRoomHost      the user is not the host
     * @throws UndercoverException.NotEnoughPlayers fewer members than a match needs
     * @throws UndercoverException.AlreadyInMatch   a member still sits at a match,
     *                                              this room's own one included
     */
    public synchronized UndercoverMatchEntity startMatch(UUID userId) {
        UndercoverRoom room = hostedRoom(userId);
        if (room.size() < UndercoverRules.MIN_PLAYERS) {
            throw new UndercoverException.NotEnoughPlayers(UndercoverRules.MIN_PLAYERS);
        }

        UndercoverMatchEntity match = undercoverService.createMatch(room.getMembers(), room.getMode(),
                room.getMode() == UndercoverGameMode.CUSTOM ? room.effectiveRules() : null,
                room.getCategories(), room.getCustomWordPairs());

        // The room outlives the match, so the idle sweeper must not carry it
        // off while the table is busy playing.
        room.touch(clock.instant());
        playerChannel.matchUpdated(match.getUndercoverMatchId());
        return match;
    }

    /**
     * @return the room this user is in, if any
     */
    public synchronized Optional<UndercoverRoom.View> roomOf(UUID userId) {
        return Optional.ofNullable(roomCodeByUser.get(userId))
                .map(roomsByCode::get)
                .map(UndercoverRoom::view);
    }

    /**
     * Drops rooms left idle, so abandoned ones do not pile up in memory.
     */
    @Scheduled(fixedRate = 600_000)
    public synchronized void dropIdleRooms() {
        Instant limit = clock.instant().minus(IDLE_TIMEOUT);
        roomsByCode.values().removeIf(room -> {
            if (room.getLastActivity().isAfter(limit)) {
                return false;
            }
            room.getMembers().forEach(roomCodeByUser::remove);
            log.info("Undercover room {} dropped after staying idle", room.getCode());
            return true;
        });
    }

    private void leaveCurrentRoom(UUID userId) {
        String code = roomCodeByUser.remove(userId);
        if (code == null) {
            return;
        }
        UndercoverRoom room = roomsByCode.get(code);
        if (room == null) {
            return;
        }
        room.remove(userId, clock.instant());
        if (room.isEmpty()) {
            roomsByCode.remove(code);
            return;
        }
        playerChannel.roomUpdated(room.view());
    }

    private UndercoverRoom hostedRoom(UUID userId) {
        UndercoverRoom room = Optional.ofNullable(roomCodeByUser.get(userId))
                .map(roomsByCode::get)
                .orElseThrow(UndercoverException.NotInRoom::new);
        if (!room.isHost(userId)) {
            throw new UndercoverException.NotRoomHost();
        }
        return room;
    }

    private void requireNotInMatch(UUID userId) {
        if (matchRepository.existsActiveMatch(userId)) {
            throw new UndercoverException.AlreadyInMatch(userId);
        }
    }

    private String newCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (roomsByCode.containsKey(code));
        return code;
    }
}
