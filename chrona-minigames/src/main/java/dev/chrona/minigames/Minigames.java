package dev.chrona.minigames;

import dev.chrona.minigames.core.MinigameManager;
import dev.chrona.minigames.core.types.PressQTEGame;
import dev.chrona.minigames.core.types.TimingBarGame;
import org.bukkit.plugin.Plugin;

/** Einfache Facade, damit andere Module leicht drankommen. */
public final class Minigames {
    private static MinigameManager mgr;

    public static void init(Plugin plugin) {
        mgr = new MinigameManager(plugin);
        mgr.register(new TimingBarGame(plugin));
        mgr.register(new PressQTEGame(plugin));
    }

    public static MinigameManager manager() {
        return mgr;
    }

    public static void shutdown() {
        if (mgr != null)
            mgr.shutdown();
        mgr = null;
    }
}
