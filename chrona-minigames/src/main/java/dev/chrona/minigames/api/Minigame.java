package dev.chrona.minigames.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Minigame {
    String id(); // e.g. "TIMING_BAR"

    /**
     * Startet das Minigame für einen Spieler.
     * @param player  Zielspieler
     * @param data    konfig (z.B. "speed", "windowStart", "windowEnd")
     * @return Future, die bei Ende ein Result liefert
     */
    CompletableFuture<MinigameResult> start(Player player, Map<String, Object> data);
}


