package dev.chrona.plugin;

import dev.chrona.common.Db;
import dev.chrona.economy.infrastructure.JdbcWalletRepository;
import dev.chrona.economy.domain.Wallet;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.concurrent.CompletableFuture;

public final class PlayerJoinListeners implements Listener {

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        var id = e.getUniqueId();
        var name = e.getName();

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
                ex.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();

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
                ex.printStackTrace();
            }
        });
    }
}
