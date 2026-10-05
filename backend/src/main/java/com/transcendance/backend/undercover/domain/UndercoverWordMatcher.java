package com.transcendance.backend.undercover.domain;

import java.text.Normalizer;
import java.util.Locale;

public class UndercoverWordMatcher {

    /**
     * Reduces a word to the form guesses are compared in.
     *
     * Only letters and digits survive: "Counter-Strike", "counter strike" and
     * "COUNTERSTRIKE" all name the same word.
     *
     * @param word the word as typed, or null when nothing was sent
     * @return lower case, without accents, spaces or punctuation; an empty
     *         string for null
     */
    public String normalize(String word) {
        if (word == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(word, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        // Locale.ROOT: under a Turkish default locale, "I" would lower to a
        // dotless "ı" and stop matching.
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /**
     * @param guess      what Mr White sent, or null if his time ran out
     * @param agentsWord the word the agents were given
     * @return whether the guess names that word; never true for a blank guess
     */
    public boolean matches(String guess, String agentsWord) {
        String normalizedGuess = normalize(guess);
        if (normalizedGuess.isEmpty()) {
            return false;
        }
        return normalizedGuess.equals(normalize(agentsWord));
    }
}
