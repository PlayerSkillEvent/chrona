package dev.chrona.common.npc.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NpcService {
    NpcHandle create(Location loc, String name, Skin skin);
    NpcHandle createFor(Player viewer, Location loc, String name, Skin skin);
    void registerListener(NpcClickListener listener);
    void unregisterListener(NpcClickListener listener);
}
