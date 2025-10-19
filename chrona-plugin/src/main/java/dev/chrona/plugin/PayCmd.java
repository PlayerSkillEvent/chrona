package dev.chrona.plugin;

import dev.chrona.common.economy.EconomySource;
import dev.chrona.common.economy.TransactionOrigin;
import dev.chrona.economy.infrastructure.JdbcTransferService;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PayCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p))
            return true;

        if (a.length != 2) {
            p.sendMessage("§cUsage: /pay <player> <amount>");
            return true;
        }

        var target = p.getServer().getPlayerExact(a[0]);
        if (target == null) {
            p.sendMessage("§cPlayer not found");
            return true;
        }

        long amt;
        try {
            amt = Long.parseLong(a[1]);
        }
        catch (Exception e) {
            p.sendMessage("§cInvalid amount");
            return true;

        }

        if (amt <= 0) {
            p.sendMessage("§cAmount must be > 0");
            return true;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage("§cNo self-pay");
            return true;
        }

        var svc = new JdbcTransferService();
        var transferId = UUID.randomUUID();
        var origin = TransactionOrigin.of(EconomySource.PLAYER_TRADE);
        CompletableFuture.runAsync(() -> svc.transfer(p.getUniqueId(), target.getUniqueId(), amt, transferId, origin))
                .whenComplete((ok, ex) -> p.getServer().getScheduler().runTask(p.getServer().getPluginManager().getPlugin("Chrona"), () -> {
                    if (ex != null)
                        p.sendMessage("§c/pay failed: " + ex.getMessage());
                    else {
                        p.sendMessage("§aSent §e" + amt + " §ato §6" + target.getName());
                        target.sendMessage("§aReceived §e" + amt + " §afrom §6" + p.getName());
                    }
                }));
        return true;
    }
}
