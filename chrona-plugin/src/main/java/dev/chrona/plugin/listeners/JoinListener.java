package dev.chrona.plugin.listeners;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.economy.PlayerRepo;
import dev.chrona.plugin.ChronaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;

import java.util.List;

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
        var player = e.getPlayer();
        var id = player.getUniqueId();
        var name = player.getName();
        player.locale();
        var locale = player.locale().toLanguageTag();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repo.ensurePlayerAndWallet(id, name, locale);
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
