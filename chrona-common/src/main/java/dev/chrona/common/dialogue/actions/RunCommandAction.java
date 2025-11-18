package dev.chrona.common.dialogue.actions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public final class RunCommandAction implements DialogueAction {

    @Override
    public String type() {
        return "run_command";
    }

    @Override
    public void execute(Player player, NpcHandle npc, DialogueSession session, ActionDef def) {
        var params = def.params();
        String cmd = params.has("command") ? params.get("command").getAsString() : null;
        String executor = params.has("executor") ? params.get("executor").getAsString().toUpperCase() : "CONSOLE";
        if (cmd == null)
            return;

        cmd = cmd.replace("%player%", player.getName());

        if (executor.equals("PLAYER"))
            player.performCommand(cmd);
        else
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
    }
}

