package dev.chrona.plugin.commands;

import dev.chrona.job.jobs.miner.MinerItems;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MinerGiveCmd implements CommandExecutor {
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("players only");
            return true;
        }

        player.getInventory().addItem(MinerItems.waterStrike());
        player.sendMessage("§bWasserschlag §7erhalten.");
        return true;
    }
}
