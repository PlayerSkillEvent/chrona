package dev.chrona.plugin.commands;

import dev.chrona.common.economy.SystemSource;
import dev.chrona.common.economy.TransactionOrigin;
import dev.chrona.economy.infrastructure.JdbcWalletRepository;
import dev.chrona.economy.infrastructure.JdbcWalletService;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GrantCmd implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, String[] a) {
        if (!s.hasPermission("chrona.grant")) {
            s.sendMessage("§cNo permission");
            return true;

        }
        if (a.length != 2) {
            s.sendMessage("§cUsage: /grant <player> <amount>");
            return true;
        }

        var target = s.getServer().getPlayerExact(a[0]);
        if (target == null) {
            s.sendMessage("§cPlayer not found");
            return true;
        }

        long amt;
        try {
            amt = Long.parseLong(a[1]);
        }
        catch (Exception e) {
            s.sendMessage("§cInvalid amount");
            return true;
        }

        if (amt <= 0) {
            s.sendMessage("§cAmount must be > 0");
            return true;
        }

        CompletableFuture.runAsync(() -> {
            var service = new JdbcWalletService(new JdbcWalletRepository());
            var id = target.getUniqueId();

            service.credit(id, amt, UUID.randomUUID(), TransactionOrigin.of(SystemSource.ADMIN_GRANT));
        }).whenComplete((ok, ex) -> s.getServer().getScheduler().runTask(Objects.requireNonNull(s.getServer().getPluginManager().getPlugin("Chrona")), () -> {
            if (ex != null)
                s.sendMessage("§c/grant failed: " + ex.getMessage());
            else {
                s.sendMessage("§aGranted §e" + amt + " §ato §6" + target.getName());
                target.sendMessage("§aAdmin granted §e" + amt);
            }
        }));
        return true;
    }
}
