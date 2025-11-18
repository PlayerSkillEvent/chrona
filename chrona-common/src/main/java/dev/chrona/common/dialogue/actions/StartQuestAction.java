package dev.chrona.common.dialogue.actions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public final class StartQuestAction implements DialogueAction {

    @Override
    public String type() {
        return "start_quest";
    }

    @Override
    public void execute(Player player, NpcHandle npc, DialogueSession session, ActionDef def) {
        var params = def.params();

        String id = params.has("id") ? params.get("id").getAsString() : null;
        if (id == null)
            return;

        player.sendMessage("§7[Quest] §fStarted quest §e" + id + "§f (stub).");
        // TODO: hook into real quest system
    }
}
