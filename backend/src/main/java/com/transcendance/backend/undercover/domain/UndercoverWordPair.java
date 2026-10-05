package com.transcendance.backend.undercover.domain;

/**
 * Two close words, one for the agents and one for the undercover. The seeded
 * pairs live in the database; this is the form a host's own list takes.
 */
public record UndercoverWordPair(String agentsWord, String undercoverWord) {

    /** A list long enough for many matches, short enough to stay a form. */
    public static final int MAX_CUSTOM_PAIRS = 50;

    public static final int MAX_WORD_LENGTH = 40;
}
