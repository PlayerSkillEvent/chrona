package dev.chrona.common.dialogue.actions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public final class RunCommandAction implements DialogueAction {
    @Override public String type() { return "run_command"; }

    @Override
    public void execute(Player player, NpcHandle npc, DialogueSession session, ActionDef def) {
        var p = def.params();
        String cmd = p.has("command") ? p.get("command").getAsString() : null;
        String executor = p.has("executor") ? p.get("executor").getAsString().toUpperCase() : "CONSOLE";
        if (cmd == null) return;

        cmd = cmd.replace("%player%", player.getName());

        switch (executor) {
            case "PLAYER" -> player.performCommand(cmd);
            default -> player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
        }
    }
}

