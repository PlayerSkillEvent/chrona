package dev.chrona.plugin.commands;

import dev.chrona.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class PayCmd implements CommandExecutor {

    private final EconomyService econ;

    public PayCmd(EconomyService e) {
        this.econ = e;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String lbl, String[] a) {
        if (!(s instanceof Player p)) {
            return true;
        }

        if (a.length != 2) {
            p.sendMessage("§7Use: /pay <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(a[0]);
        if (target == null) {
            p.sendMessage("§cPlayer not found.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(a[1]);
        } catch (NumberFormatException e) {
            p.sendMessage("§cInvalid amount.");
            return true;
        }

        if (amount <= 0) {
            p.sendMessage("§cAmount must be > 0.");
            return true;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage("§cYou cannot pay yourself.");
            return true;
        }

        try {
            long newBalance = econ.pay(p.getUniqueId(), target.getUniqueId(), amount);
            p.sendMessage("§aPaid §e" + amount + " Ð §ato §6" + target.getName() + " §8- New balance: §e" + newBalance + " Ð");
            target.sendMessage("§6" + p.getName() + " §apaid you §e" + amount + " Ð");
        } catch (SQLException ex) {
            p.sendMessage("§cPayment failed.");
        }
        return true;
    }
}

