package dev.chrona.plugin.listeners;

import dev.chrona.common.Db;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.log.ChronaMarkers;
import dev.chrona.economy.infrastructure.JdbcWalletRepository;
import dev.chrona.economy.domain.Wallet;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class PlayerJoinListeners implements Listener {

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        var id = e.getUniqueId();
        var name = e.getName();

        var log = ChronaLog.get(PlayerJoinListeners.class);

        CompletableFuture.runAsync(() -> {
            try (var con = Db.ds().getConnection();
                 var ps = con.prepareStatement("""
             insert into player(id, name) values (?,?)
             on conflict (id) do update set name=excluded.name, updated_at=now()
           """)) {
                ps.setObject(1, id);
                ps.setString(2, name);
                ps.executeUpdate();
            }
            catch (Exception ex) {
                ChronaLog.error(log, ChronaMarkers.AUDIT, ex, "Command execution failed");
            }
        });
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent e) {
        var p = e.getPlayer();

        var log = ChronaLog.get(PlayerJoinListeners.class);

        e.joinMessage(null);

        CompletableFuture.runAsync(() -> {
            var repo = new JdbcWalletRepository();
            var w = repo.find(p.getUniqueId()).orElse(null);
            if (w == null)
                repo.insert(new Wallet(p.getUniqueId(), 0, 0));

            try (var con = Db.ds().getConnection();
                var ps = con.prepareStatement("update player set last_seen=now() where id=?")) {
                ps.setObject(1, p.getUniqueId()); ps.executeUpdate();
            }
            catch (Exception ex) {
                ChronaLog.error(log, ChronaMarkers.AUDIT, ex, "Command execution failed");
            }
        });
    }
}
