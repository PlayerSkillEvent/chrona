package dev.chrona.job.jobs.miner;

import dev.chrona.common.hologram.api.HologramHandle;
import dev.chrona.common.hologram.protocol.ProtocolHolograms;
import dev.chrona.common.log.ChronaLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/** Eine Glutstelle an einer Blockposition. Level 0..3, Hologram pro Viewer. */
final class Firepit {
    private final ProtocolHolograms holo;
    private final FirepitKey key;
    private int level;
    private final Map<UUID, HologramHandle> holos = new HashMap<>();

    Firepit(ProtocolHolograms holo, FirepitKey key, int level) {
        this.holo = holo;
        this.key = key;
        this.level = level;
    }

    int level() {
        return level;
    }

    void increase() {
        level = Math.min(3, level + 1);
    }

    void reset() {
        level = 0;
    }

    String label() {
        String color = switch (level) {
            case 1 -> "§e";
            case 2 -> "§6";
            case 3 -> "§c";
            default -> "§7";
        };
        return color + "Glut " + level + "/3";
    }

    Location baseLocation() {
        var w = Bukkit.getWorld(key.world());
        if (w == null)
            return null;
        return new Location(w, key.x() + 0.5, key.y(), key.z() + 0.5);
    }

    /** Zeigt/aktualisiert das Hologram NUR für diesen Spieler. */
    void ensureHologramFor(Player p) {
        var loc = baseLocation();
        if (loc == null)
            return;

        var h = holos.get(p.getUniqueId());

        if (h == null) {
            h = holo.createFor(p, loc, List.of(label()));
            holos.put(p.getUniqueId(), h);
        }
        else
            h.setLines(List.of(label()));
    }

    void hideFor(Player p) {
        var h = holos.remove(p.getUniqueId());
        if (h != null)
            h.destroy();
    }

    void destroyAllHolograms() {
        for (var h : holos.values())
            h.destroy();
        holos.clear();
    }
}
