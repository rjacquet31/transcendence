package com.transcendance.backend.undercover.domain;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class UndercoverRoleDealer {

    private final Random random;


    public UndercoverRoleDealer() {
        this(new SecureRandom());
    }

    public UndercoverRoleDealer(Random random) {
        this.random = random;
    }

    public record SeatAssignment(UUID userId, UndercoverRole role, int seatOrder) {
    }

    public List<SeatAssignment> dealAssignment(List<UUID> userIds, UndercoverRules rules) {
        rules.validateFor(userIds.size());
        if (!noDuplicateUser(userIds)) {
            throw new UndercoverException.DuplicatePlayer();
        }
        int n = userIds.size();
        List<SeatAssignment> assignments = new ArrayList<>();

        List<UUID> players = new ArrayList<>(userIds);
        Collections.shuffle(players, random);

        List<Integer> freeSeats = new ArrayList<>();
        for (int s = 1; s < n; s++)
            freeSeats.add(s);
        Collections.shuffle(freeSeats, random);

        int mrWhiteCount = rules.getMrWhiteCount();
        for (int i = 0; i < mrWhiteCount; i++) {
            int seat = freeSeats.remove(freeSeats.size() - 1);
            assignments.add(new SeatAssignment(players.get(i), UndercoverRole.MR_WHITE, seat));
        }

        freeSeats.add(0);
        Collections.shuffle(freeSeats, random);

        int undercover = rules.getUndercoverCount();
        for (int i = mrWhiteCount; i < mrWhiteCount + undercover; i++) {
            int seat = freeSeats.remove(freeSeats.size() - 1);
            assignments.add(new SeatAssignment(players.get(i), UndercoverRole.UNDERCOVER, seat));
        }

        for (int i = mrWhiteCount + undercover; i < n; i++) {
            int seat = freeSeats.remove(freeSeats.size() - 1);
            assignments.add(new SeatAssignment(players.get(i), UndercoverRole.AGENT, seat));
        }

        return assignments.stream()
        .sorted(Comparator.comparingInt(SeatAssignment::seatOrder))
        .toList();

    }

    private boolean noDuplicateUser(List<UUID> userIds) {
        return userIds.stream().distinct().count() == userIds.size();
    }
}
