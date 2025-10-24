package dev.chrona.minigames.core.types;

import dev.chrona.minigames.api.Minigame;
import dev.chrona.minigames.api.MinigameResult;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TimingBarGame implements Minigame, Listener {
    private final Plugin plugin;
    public TimingBarGame(Plugin plugin) { this.plugin = plugin; }

    @Override public String id() { return "TIMING_BAR"; }

    @Override
    public CompletableFuture<MinigameResult> start(Player p, Map<String, Object> data) {
        var result = new CompletableFuture<MinigameResult>();

        double windowStart = getD(data, "windowStart", 0.40);
        double windowEnd   = getD(data, "windowEnd",   0.60);
        double speedPerTick = getD(data, "speed", 0.018); // ~2s hin/retour
        int timeoutTicks = (int) Math.round(getD(data, "timeoutTicks", 200)); // 10s

        var bar = Bukkit.createBossBar("§eClick to stop!", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        bar.addPlayer(p);

        class State {
            double prog = 0.0;
            boolean dirRight = true;
            int ticks = 0;
            long startMs = System.currentTimeMillis();
        }

        var st = new State();

        Listener local = new Listener() {
            @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
            public void onInteract(PlayerInteractEvent e) {
                if (!e.getPlayer().getUniqueId().equals(p.getUniqueId()))
                    return;

                complete.run();
            }
        };
        Bukkit.getPluginManager().registerEvents(local, plugin);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (result.isDone()) return;
            st.ticks++;
            // move
            st.prog += st.dirRight ? speedPerTick : -speedPerTick;
            if (st.prog >= 1.0) { st.prog = 1.0; st.dirRight = false; }
            if (st.prog <= 0.0) { st.prog = 0.0; st.dirRight = true; }
            bar.setProgress(Math.max(0.0, Math.min(1.0, st.prog)));

            // timeout
            if (st.ticks >= timeoutTicks) {
                cleanup();
                result.complete(MinigameResult.fail(System.currentTimeMillis()-st.startMs));
            }
        }, 1L, 1L);

        Runnable complete = () -> {
            if (result.isDone()) return;
            boolean hit = st.prog >= windowStart && st.prog <= windowEnd;
            double center = (windowStart + windowEnd)/2.0;
            double acc = 1.0 - Math.min(1.0, Math.abs(st.prog - center) / Math.max(1e-6, (windowEnd-windowStart)/2.0));
            int score = (int) Math.round(100 * acc);
            cleanup();
            result.complete(hit ? MinigameResult.success(score, acc, System.currentTimeMillis()-st.startMs)
                    : MinigameResult.fail(System.currentTimeMillis()-st.startMs));
        };

        voidCleanup = new Runnable() { @Override public void run() { // no-op placeholder replaced below
        }};
        Runnable finalComplete = complete; // capture

        // link function for event
        this.complete = finalComplete;

        voidCleanup = () -> {
            try { bar.removeAll(); } catch (Exception ignore) {}
            HandlerList.unregisterAll(local);
            Bukkit.getScheduler().cancelTask(taskId);
        };

        return result;
    }

    // hack to allow lambda captures in older java
    private Runnable voidCleanup = () -> {};
    private Runnable complete = () -> {};
    private void cleanup() { voidCleanup.run(); }

    private static double getD(Map<String,Object> m, String k, double def) {
        if (m == null) return def;
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}

