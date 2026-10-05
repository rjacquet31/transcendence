package com.transcendance.backend.chess.domain;

public record BotProfile(int depth,int evalNoise, double blunderChance) {

    private static final BotProfile[] LEVELS = {
        new BotProfile(1, 300, 0.30),
        new BotProfile(1, 250, 0.20),
        new BotProfile(2, 200, 0.15),
        new BotProfile(2, 150, 0.10),
        new BotProfile(3, 100, 0.05),
        new BotProfile(3, 50, 0.02),
        new BotProfile(4, 25, 0.01),
        new BotProfile(4, 10, 0.005),
        new BotProfile(4, 5, 0.001),
        new BotProfile(4, 0, 0)
    };

    public static BotProfile ofLevel(int level) {
        return LEVELS[Math.clamp(level, 1, 10) - 1];
    }
}
