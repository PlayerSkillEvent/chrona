package dev.chrona.plugin.commands;

import dev.chrona.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.UUID;

public class EconCmd implements CommandExecutor {

    private final EconomyService econ;

    public EconCmd(EconomyService econ) {
        this.econ = econ;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        if (!(args.length == 3)) {
            player.sendMessage("§7Use: /" + label + " <mint|burn> <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return true;
        }

        long amount = Long.parseLong(args[2]);

        switch (args[0].toLowerCase()) {
            case "mint" -> {
                try {
                    long newBalance = econ.mint(player.getUniqueId(), target.getUniqueId(), amount, UUID.randomUUID());
                    target.sendMessage("§aYour wallet has been minted §e" + amount + " Ð §aby an admin. New balance: §e" + newBalance + " Ð");
                }
                catch (SQLException e) {
                    sender.sendMessage("§cError");
                }
            }
            case "burn" -> {
                try {
                    long newBalance = econ.burn(player.getUniqueId(), target.getUniqueId(), amount, UUID.randomUUID());
                    target.sendMessage("§aYour wallet has been burned §e" + amount + " Ð §aby an admin. New balance: §e" + newBalance + " Ð");
                }
                catch (SQLException e) {
                    sender.sendMessage("§cError");
                }
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use: /" + label + " <mint|burn> <player> <amount>");
        }
        return true;
    }
}
