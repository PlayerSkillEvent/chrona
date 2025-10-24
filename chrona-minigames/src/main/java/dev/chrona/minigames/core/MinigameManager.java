package dev.chrona.minigames.core;

import dev.chrona.minigames.api.Minigame;
import dev.chrona.minigames.api.MinigameResult;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class MinigameManager {
    private final Plugin plugin;
    private final Map<String, Minigame> registry = new HashMap<>();
    private final Map<UUID, MinigameSession> active = new HashMap<>();
    private final MinigameRouterListener router;

    public MinigameManager(Plugin plugin) {
        this.plugin = plugin;
        this.router = new MinigameRouterListener(this);
        Bukkit.getPluginManager().registerEvents(router, plugin);
    }

    public void register(Minigame game) { registry.put(game.id(), game); }
    public Optional<Minigame> get(String id) { return Optional.ofNullable(registry.get(id)); }

    public CompletableFuture<MinigameResult> start(String id, Player p, Map<String,Object> data) {
        var game = registry.get(id);
        if (game == null) throw new IllegalArgumentException("Unknown minigame: " + id);
        cancelActive(p.getUniqueId(), "replaced");
        var future = game.start(p, data);
        var session = new MinigameSession(id, p.getUniqueId(), System.currentTimeMillis(), future);
        active.put(p.getUniqueId(), session);
        future.whenComplete((r, ex) -> active.remove(p.getUniqueId()));
        return future;
    }

    public Optional<MinigameSession> session(UUID playerId) { return Optional.ofNullable(active.get(playerId)); }
    public void cancelActive(UUID playerId, String reason) {
        var s = active.remove(playerId);
        if (s != null) s.cancel(reason);
    }

    public void shutdown() {
        active.values().forEach(s -> s.cancel("plugin-disable"));
        active.clear();
        HandlerList.unregisterAll(router);
    }

    Plugin plugin() { return plugin; }
}

