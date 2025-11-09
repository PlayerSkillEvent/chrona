package dev.chrona.plugin.listeners;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.api.NpcHandle;
import dev.chrona.common.npc.api.Skin;
import dev.chrona.economy.PlayerRepo;
import dev.chrona.plugin.ChronaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;

import java.util.UUID;

public final class JoinListener implements Listener {
    private final ChronaPlugin plugin;
    private final PlayerRepo repo;
    private final Logger logger;

    public JoinListener(ChronaPlugin plugin, PlayerRepo repo) {
        this.logger = ChronaLog.get(JoinListener.class);
        this.plugin = plugin;
        this.repo = repo;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String locale = player.locale().toLanguageTag();

        Location npcLoc = new Location(player.getWorld(), -100, 69, 110);

        NpcHandle npc = plugin.getNpcs().create(
                npcLoc,
                "Eliasss",
                new Skin("ewogICJ0aW1lc3RhbXAiIDogMTc2MTU5MTgyMDI4MiwKICAicHJvZmlsZUlkIiA6ICJmMzFkMDdkMmM2MmM0MzI0YjRkZTUzZjYyYmFkMzA4YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJ4aW1yeGFuZHkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFhNjhiYzNhZTczZWQ4NDc2ZjM4NTc1YjMyZTllNDNhZTk2NzRiNTA0OGE5ZmQwZjcyMWYzMDAxY2QzZjc3YiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "MC8t647y3cpilgIWSO2d1SpntczPyTXAvyIzAnX3SdaohkTh/57BzON5OTTwajZ1wCt0C+cSlEoP9IFAQMvHdCgGSPbKtp56jnv/834KPEwDFp7ccBRMCQnwtGTmWG2pEvaRCHXdAZI9sT2YPxVs/oAWGEX8CvpUPvRsnxBgj0JUhPEz9dfAoldsCENjbI3VJ3ivZ9GNnEgRan3/j6jFf6mtGF0gsufPoCH+rO2nLwDHMrCM1irKTmieZSVBvkMWHbx8C+D7rYy4tVxZ1Z5Naah3vT4NS5jP57kfCT3eWXooDR4MuSrE76llvjGxexG+EUyID97IjXXmc8c2DBVm9kB5gSciedaVYoEhY4fhApKTlxxNRU+HIM90lX26IKbMfVQ73CEq6YMX8/1xn5ZslrgARTpvb8a/vD3iJJ8bzFqPdJ/NtrB/lhZK8bbilz3E7s+nDoASnkxHrpoGSmoc9X5gKZCvPLpGCkMnO+jeyg4SA/I3E6tV/jXK1pHFN87tJONdIQKfDsFdZBVy5paS0ljKTgwHYqVcg9Od1VpCIOzxHCzVXhHKI6GGm8oZtUGx3mSkk5LsV5u2SuvItwfB+1UzPDO+Ag6E0uSYTgOI5FV73l5+797iwzVUDqdkm+xdvrtexONYTpzwOHwSAq/U6d31mFHC0NBYvN/KQ0cNAdA=")
        );

        Bukkit.getScheduler().runTask(plugin, () -> {
            npc.addViewer(player);

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        this.cancel();
                        npc.removeViewer(player);
                        npc.destroy();
                        return;
                    }
                    if (player.getWorld() != npcLoc.getWorld()) return;

                    if (player.getLocation().distanceSquared(npcLoc) <= 12 * 12)
                        npc.lookAt(player, player.getEyeLocation());
                }
            };
            task.runTaskTimer(plugin, 10L, 2L);

            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onQuit(PlayerQuitEvent qe) {
                    if (!qe.getPlayer().getUniqueId().equals(uuid)) return;
                    task.cancel();
                    npc.removeViewer(qe.getPlayer());
                    npc.destroy();
                    PlayerQuitEvent.getHandlerList().unregister(this);
                }
            }, plugin);
        });

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repo.ensurePlayerAndWallet(uuid, name, locale);
            } catch (Exception ex) {
                logger.info("Failed to ensure player/wallet for {}", name, ex);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var id = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repo.touchLastSeen(id);
            }
            catch (Exception ex) {
                logger.warn("Failed to touch last_seen for {}", id, ex);
            }
        });
    }
}
