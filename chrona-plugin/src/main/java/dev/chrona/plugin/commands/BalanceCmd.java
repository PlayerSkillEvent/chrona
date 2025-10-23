package dev.chrona.plugin.commands;
import dev.chrona.economy.domain.Wallet;
import dev.chrona.economy.infrastructure.JdbcWalletRepository;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class BalanceCmd implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, String[] a) {
        if(!(s instanceof Player p))
            return true;

        var repo = new JdbcWalletRepository();

        CompletableFuture.supplyAsync(() -> repo.find(p.getUniqueId()))
                .thenAccept(
                    wallet -> p.getServer().getScheduler().runTask(
                            Objects.requireNonNull(p.getServer().getPluginManager().getPlugin("Chrona")),
                            () -> {
                                long bal = wallet.map(Wallet::balance).orElse(0L);
                                p.sendMessage("§eDeben: §6" + bal);
                            }
                    )
                );

        return true;
    }
}
