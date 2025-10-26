package dev.chrona.common.hologram.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.List;

public interface HologramService {
    HologramHandle create(Location loc, List<String> lines);
    HologramHandle createFor(Player viewer, Location loc, List<String> lines);
}
