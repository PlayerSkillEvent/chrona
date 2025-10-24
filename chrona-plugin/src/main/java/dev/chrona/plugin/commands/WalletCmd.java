package dev.chrona.plugin.commands;

import dev.chrona.economy.EconomyService;
import dev.chrona.economy.Transfer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.sql.SQLException;

public final class WalletCmd implements CommandExecutor {

    private final EconomyService econ;

    public WalletCmd(EconomyService e) {
        this.econ = e;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String lbl, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        switch (args.length) {
            case 0 -> {
                try {
                    long bal = econ.getBalance(player.getUniqueId());
                    player.sendMessage("§6Balance: §e" + bal + " Ð");
                }
                catch (SQLException ex) {
                    player.sendMessage("§cError.");
                }
                return true;
            }
            case 3 -> {
                if (!args[0].equalsIgnoreCase("history") || !player.hasPermission("chrona.wallet.history")) {
                    player.sendMessage("§7Use: /" + lbl + " - check balance");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }

                int limit = Integer.parseInt(args[2]);

                try {
                    Transfer[] transfers = econ.getTransfers(target.getUniqueId(), limit, 0);
                    player.sendMessage("§6Last " + limit + " transfers for §e" + target.getName() + "§6:");
                    for (var transfer : transfers) {
                        player.sendMessage("§7[" + new Date(transfer.timestamp()) + "] " +
                                "§e" + transfer.amount() + " Ð §7from §e" + Bukkit.getOfflinePlayer(transfer.from()).getName() + " §7to §e" + Bukkit.getOfflinePlayer(transfer.to()).getName() + " " +
                                "§7(Reason: " + transfer.reason().value() + ")");
                    }
                }
                catch (SQLException e) {
                    player.sendMessage("§cError.");
                    return true;
                }
                return true;
            }
            default -> player.sendMessage("§7Use: /" + lbl + " - check balance");
        }
        return false;
    }
}
