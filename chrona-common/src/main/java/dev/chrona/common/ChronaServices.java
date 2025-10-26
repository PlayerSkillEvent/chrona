package dev.chrona.common;

import dev.chrona.common.hologram.api.HologramService;
import dev.chrona.common.hologram.protocol.ProtocolHolograms;
import dev.chrona.economy.EconomyService;
import dev.chrona.minigames.core.MinigameManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Zentraler Service-Locator für Chrona.
 * - Einmalig initialisieren (init) im Plugin-Bootstrap
 * - Thread-sicher; verhindert Doppel-Init
 * - Enthält bequeme runSync/runAsync-Helper
 */
public final class ChronaServices {
    private ChronaServices() {}

    private static volatile boolean INITIALIZED = false;

    private static JavaPlugin plugin;
    private static DataSource dataSource;
    private static EconomyService economy;
    private static ProtocolHolograms holograms;
    private static MinigameManager minigames;

    // optionaler eigener Async-Pool (für DB/IO), falls du nicht den Bukkit-Async-Scheduler nutzen willst
    private static ExecutorService ioPool;

    // ---------- Init / Shutdown ----------

    public static synchronized void init(JavaPlugin pl,
                                         DataSource ds,
                                         EconomyService econ,
                                         ProtocolHolograms holo,
                                         MinigameManager mini) {
        if (INITIALIZED) throw new IllegalStateException("ChronaServices already initialized");
        plugin     = Objects.requireNonNull(pl, "plugin");
        dataSource = Objects.requireNonNull(ds, "dataSource");
        economy    = Objects.requireNonNull(econ, "economy");
        holograms  = Objects.requireNonNull(holo, "holograms");
        minigames  = Objects.requireNonNull(mini, "minigames");

        ioPool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "chrona-io");
            t.setDaemon(true);
            return t;
        });

        INITIALIZED = true;
    }

    /** Ruft Shutdown-Hooks auf und leert alle Referenzen. */
    public static synchronized void shutdown() {
        if (!INITIALIZED) return;
        try {
            if (minigames != null) {
                try {
                    minigames.shutdown();
                }
                catch (Throwable ignore) {}
            }
            if (holograms != null) {
                try {
                    holograms.clearAll();
                } catch (Throwable ignore) {}
            }
        } finally {
            if (ioPool != null) {
                ioPool.shutdownNow();
                ioPool = null;
            }
            plugin = null;
            dataSource = null;
            economy = null;
            holograms = null;
            minigames = null;
            INITIALIZED = false;
        }
    }

    public static boolean isInitialized() { return INITIALIZED; }

    public static JavaPlugin plugin() {
        ensure();
        return plugin;
    }

    public static DataSource dataSource() {
        ensure();
        return dataSource;
    }

    public static EconomyService economy() {
        ensure();
        return economy;
    }

    public static HologramService holograms() {
        ensure();
        return holograms;
    }

    public static MinigameManager minigames() {
        ensure();
        return minigames;
    }

    // ---------- Helpers ----------

    /** Führe Code auf dem Server-Thread aus. */
    public static void runSync(Runnable r) {
        ensure();
        if (Bukkit.isPrimaryThread()) {
            r.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    /** Führe Code asynchron aus (eigener kleiner IO-Pool). */
    public static void runAsync(Runnable r) {
        ensure();
        if (ioPool != null) ioPool.execute(r);
        else Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
    }

    private static void ensure() {
        if (!INITIALIZED) throw new IllegalStateException("ChronaServices not initialized");
    }
}
