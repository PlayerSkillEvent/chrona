package dev.chrona.common.region;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener, der PlayerRegionEnterEvent/LeaveEvent hört
 * und sie an den RegionVisitLogService weiterreicht.
 */
public final class RegionVisitLogListener implements Listener {

    private final RegionVisitLogService logService;

    public RegionVisitLogListener(RegionVisitLogService logService) {
        this.logService = logService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegionEnter(PlayerRegionEnterEvent event) {
        logService.logEnter(event.getPlayer(), event.getRegion(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegionLeave(PlayerRegionLeaveEvent event) {
        logService.logLeave(event.getPlayer(), event.getRegion(), event.getTo());
    }
}
