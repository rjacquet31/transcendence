package com.transcendance.backend.undercover.api.dto;

import java.util.List;

import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.domain.UndercoverWordPair;

/**
 * Request bodies of the undercover api. Checked by the services, which refuse
 * a missing or blank value with a message the client can show.
 */
public final class UndercoverRequests {

    private UndercoverRequests() {
    }

    public record JoinRoom(String code) {
    }

    /**
     * @param rules only read for CUSTOM
     */
    public record ConfigureRoom(UndercoverGameMode mode, UndercoverRulesDto rules) {
    }

    /**
     * @param categories null or empty for every category; unused while
     *                   wordPairs holds any pair
     * @param wordPairs  the host's own list, null or empty for the seeded pairs
     */
    public record ChooseWords(List<String> categories, List<UndercoverWordPair> wordPairs) {
    }

    public record SubmitClue(String word) {
    }

    public record SubmitVote(Integer targetSeat) {
    }

    public record SubmitGuess(String guess) {
    }

    public record SendChat(String text) {
    }
}
