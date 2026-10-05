package com.transcendance.backend.undercover.domain;

import java.util.List;
import java.util.OptionalInt;

public class UndercoverTurnOrder {

    /**
     * Returns the first speaker in the turn order, or empty if there is no first
     * speaker.
     * 
     * @param aliveSeats the list of alive seats
     * @return the first speaker, or empty if there is no first speaker
     */
    public OptionalInt firstSpeaker(List<Integer> aliveSeats) {
        OptionalInt firstSeat = OptionalInt.empty();
        for (Integer seat : aliveSeats) {
            if (!firstSeat.isPresent() || seat < firstSeat.getAsInt()) {
                firstSeat = OptionalInt.of(seat);
            }
        }
        return firstSeat;
    }

    /**
     * Returns the next speaker in the turn order, or empty if there is no next
     * speaker.
     * 
     * @param aliveSeats  the list of alive seats
     * @param currentSeat the current seat
     * @return the next speaker, or empty if there is no next speaker
     */
    public OptionalInt nextSpeaker(List<Integer> aliveSeats, int currentSeat) {
        OptionalInt nextSeat = OptionalInt.empty();
        for (Integer seat : aliveSeats) {
            if (seat <= currentSeat) {
                continue;
            }
            if (!nextSeat.isPresent() || seat < nextSeat.getAsInt()) {
                nextSeat = OptionalInt.of(seat);
            }
        }
        return nextSeat;

    }
}
