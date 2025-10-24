package dev.chrona.minigames.core;

import dev.chrona.minigames.api.MinigameResult;

import java.util.concurrent.CompletableFuture;

public record MinigameSession(String id, java.util.UUID playerId, long startedAt, CompletableFuture<MinigameResult> future) {
    void cancel(String reason) {
        if (!future.isDone())
            future.complete(MinigameResult.fail(System.currentTimeMillis() - startedAt));
    }
}
