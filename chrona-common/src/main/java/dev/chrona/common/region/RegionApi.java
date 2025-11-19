package dev.chrona.common.region;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Static API for accessing region data.
 */
public final class RegionApi {

    private static RegionService INSTANCE;

    private RegionApi() {}

    /** Initializes the Region API with the given service instance. */
    public static void init(RegionService service) {
        INSTANCE = service;
    }

    /** Shuts down the Region API. */
    public static Optional<Region> getRegionById(String id) {
        if (INSTANCE == null)
            return Optional.empty();
        return INSTANCE.getRegionById(id);
    }

    /** Gets all regions at the given location. */
    public static Set<Region> getRegionsAt(Location loc) {
        if (INSTANCE == null)
            return Collections.emptySet();
        return INSTANCE.getRegionsAt(loc);
    }

    /** Checks if the player is in the region with the given ID. */
    public static boolean isInRegion(Player player, String regionId) {
        if (INSTANCE == null)
            return false;
        return INSTANCE.isInRegion(player, regionId);
    }

    /** Gets all region IDs the player is currently in. */
    public static Set<String> getPlayerRegionIds(Player player) {
        if (INSTANCE == null)
            return Collections.emptySet();
        return INSTANCE.getPlayerRegionIds(player);
    }
}
