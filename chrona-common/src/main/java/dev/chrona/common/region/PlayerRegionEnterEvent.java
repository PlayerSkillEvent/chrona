package dev.chrona.common.region;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fires when a player enters a region.
 * Not cancellable – you can only react.
 */
public class PlayerRegionEnterEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Region region;
    private final Location to;

    public PlayerRegionEnterEvent(Player who, Region region, Location to) {
        super(who);
        this.region = region;
        this.to = to;
    }

    /** Returns the region that was entered. */
    public Region getRegion() {
        return region;
    }

    /** Returns the location the player moved to. */
    public Location getTo() {
        return to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** Returns the handler list for this event type. */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
