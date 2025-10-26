package dev.chrona.common.hologram.protocol;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.chrona.common.hologram.api.HologramHandle;
import dev.chrona.common.hologram.api.HologramService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolHolograms implements HologramService {
    private final ProtocolManager pm;
    private final Map<UUID, PacketHologram> holos = new ConcurrentHashMap<>();

    public ProtocolHolograms() {
        this.pm = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public HologramHandle create(Location loc, List<String> lines) {
        var h = new PacketHologram(pm, loc, lines);
        holos.put(h.id(), h);
        return h;
    }

    @Override
    public HologramHandle createFor(Player viewer, Location loc, List<String> lines) {
        var h = new PacketHologram(pm, loc, lines);
        holos.put(h.id(), h);
        h.addViewer(viewer);
        return h;
    }

    public void clearAll() {
        holos.values().forEach(PacketHologram::destroy);
        holos.clear();
    }
}

