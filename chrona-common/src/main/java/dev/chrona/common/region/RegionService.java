package dev.chrona.common.region;

import dev.chrona.common.log.ChronaLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Region-Management.:
 * - Saves Regions loaded by Loader.
 * - Offers getters (getRegionsAt, isInRegion, getRegionById)
 * - Fires enter/leave events.
 */
public final class RegionService implements Listener {

    private final Logger log = ChronaLog.get(RegionService.class);
    private final Plugin plugin;

    // worldName -> Regions in dieser Welt
    private final Map<String, List<Region>> regionsByWorld = new HashMap<>();

    // Player -> aktuelle Regions (für Enter/Leave-Erkennung)
    private final Map<UUID, Set<String>> playerRegions = new ConcurrentHashMap<>();

    public RegionService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * gets called by Loader to set all Regions.
     */
    public synchronized void setRegions(Collection<Region> regions) {
        regionsByWorld.clear();
        for (Region region : regions) {
            regionsByWorld
                    .computeIfAbsent(region.worldName(), w -> new ArrayList<>())
                    .add(region);
        }

        // sort regions by priority descending
        for (List<Region> list : regionsByWorld.values())
            list.sort(Comparator.comparingInt(Region::priority).reversed());

        log.info("RegionService geladen: {} Regionen über {} Welten.",
                regions.size(), regionsByWorld.size());

        // recalc for all online players
        for (Player player : Bukkit.getOnlinePlayers())
            recalcPlayerRegions(player, player.getLocation(), true);
    }

    /** Gets a Region by its ID, if it exists. */
    public Optional<Region> getRegionById(String id) {
        for (List<Region> regions : regionsByWorld.values()) {
            for (Region region : regions) {
                if (region.id().equalsIgnoreCase(id)) {
                    return Optional.of(region);
                }
            }
        }
        return Optional.empty();
    }

    /** Gets all Regions at the given Location. */
    public Set<Region> getRegionsAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return Collections.emptySet();

        List<Region> worldRegions = regionsByWorld.get(loc.getWorld().getName());
        if (worldRegions == null || worldRegions.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Region> result = new LinkedHashSet<>();
        for (Region region : worldRegions) {
            if (region.contains(loc)) {
                result.add(region);
            }
        }
        return result;
    }

    /** Checks if the Player is currently in the given Region ID. */
    public boolean isInRegion(Player player, String regionId) {
        Set<String> current = playerRegions.get(player.getUniqueId());
        if (current == null) return false;
        return current.contains(regionId);
    }

    /** Gets the IDs of all Regions the Player is currently in. */
    public Set<String> getPlayerRegionIds(Player player) {
        return playerRegions.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    // ---------------- Event-Handling ----------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Mini-Optimization: Nur handeln, wenn sich Block oder Welt geändert hat
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        recalcPlayerRegions(event.getPlayer(), to, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        recalcPlayerRegions(event.getPlayer(), to, false);
    }

    /** Recalculates the Regions a Player is in, and fires enter/leave events. */
    private void recalcPlayerRegions(Player player, Location to, boolean silent) {
        UUID uuid = player.getUniqueId();
        Set<String> oldRegions = playerRegions.getOrDefault(uuid, Collections.emptySet());
        Set<String> newRegions = new LinkedHashSet<>();

        for (Region region : getRegionsAt(to)) {
            newRegions.add(region.id());
        }

        // Enter: in newRegions aber nicht in oldRegions
        Set<String> entered = new LinkedHashSet<>(newRegions);
        entered.removeAll(oldRegions);

        // Leave: in oldRegions aber nicht in newRegions
        Set<String> left = new LinkedHashSet<>(oldRegions);
        left.removeAll(newRegions);

        if (!entered.isEmpty() || !left.isEmpty()) {
            playerRegions.put(uuid, newRegions);
        }

        if (silent) {
            return;
        }

        for (String regionId : entered) {
            Region region = getRegionById(regionId).orElse(null);
            if (region == null) continue;

            PlayerRegionEnterEvent enterEvent =
                    new PlayerRegionEnterEvent(player, region, to);
            Bukkit.getPluginManager().callEvent(enterEvent);
        }

        for (String regionId : left) {
            Region region = getRegionById(regionId).orElse(null);
            if (region == null) continue;

            PlayerRegionLeaveEvent leaveEvent =
                    new PlayerRegionLeaveEvent(player, region, to);
            Bukkit.getPluginManager().callEvent(leaveEvent);
        }
    }
}
