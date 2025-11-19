package dev.chrona.common.region;

import dev.chrona.common.Db;
import dev.chrona.common.log.ChronaLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * Service der Region-Enter/Leave-Ereignisse in die DB loggt.
 *
 * - Inserts laufen asynchron, um den Main-Thread nicht zu blockieren.
 * - Sehr simpler Ansatz: 1 Insert pro Event. Für deine Playerzahlen reicht das locker.
 *   Wenn du irgendwann >500 Spieler/Server hast, kann man auf Batching umbauen.
 */
public final class RegionVisitLogService {

    private final Logger log = ChronaLog.get(RegionVisitLogService.class);
    private final Plugin plugin;
    private final DataSource ds;

    public RegionVisitLogService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.ds = Db.ds();
    }

    public void logEnter(Player player, Region region, Location to) {
        logVisit(player, region, to, "ENTER");
    }

    public void logLeave(Player player, Region region, Location to) {
        logVisit(player, region, to, "LEAVE");
    }

    private void logVisit(Player player, Region region, Location loc, String action) {
        if (player == null || region == null || loc == null || loc.getWorld() == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        String regionId = region.id();
        String worldName = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Asynchron, damit wir den Main-Thread nicht blockieren
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_region_log " +
                    "(player_id, region_id, action, world, x, y, z) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setObject(1, playerId);
                ps.setString(2, regionId);
                ps.setString(3, action);
                ps.setString(4, worldName);
                ps.setInt(5, x);
                ps.setInt(6, y);
                ps.setInt(7, z);

                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("Fehler beim Logging von Region-Visit (player={}, region={}, action={}): {}",
                        playerId, regionId, action, e.getMessage(), e);
            }
        });
    }
}
