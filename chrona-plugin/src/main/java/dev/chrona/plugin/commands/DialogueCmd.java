package dev.chrona.plugin.commands;

import dev.chrona.common.dialogue.DialogueService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DialogueCmd implements CommandExecutor {
    private final DialogueService dialogue;

    public DialogueCmd(DialogueService dialogue) {
        this.dialogue = dialogue;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("choose")) {
            if (args.length != 3) {
                player.sendMessage("§cUsage: /dialogue choose <sessionId> <choiceId>");
                return true;
            }
            dialogue.choose(player, args[1], args[2]);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("chrona.dialogue.admin")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            dialogue.reload();
            player.sendMessage("§aDialogue configuration reloaded.");
            return true;
        }

        player.sendMessage("§7Usage: /dialogue choose <sessionId> <choiceId>");
        return true;
    }
}
