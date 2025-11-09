package dev.chrona.common.npc.api;

import org.bukkit.Location;

public final class Waypoint {
    public final Location loc;
    public final long waitMs;

    public Waypoint(Location loc) {
        this(loc, 0);
    }

    public Waypoint(Location loc, long waitMs) {
        this.loc = loc.clone();
        this.waitMs = waitMs;
    }

    public Waypoint waitMs(long ms) {
        return new Waypoint(loc, ms);
    }
}
