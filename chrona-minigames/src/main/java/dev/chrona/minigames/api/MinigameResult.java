package dev.chrona.minigames.api;

public record MinigameResult(boolean success, int score, double accuracy, long elapsedMs) {

    public static MinigameResult success(int score, double acc, long ms) {
        return new MinigameResult(true, score, acc, ms);
    }

    public static MinigameResult fail(long ms) {
        return new MinigameResult(false, 0, 0.0, ms);
    }
}

