package dev.chrona.common.dialogue.actions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public interface DialogueAction {
    String type();
    void execute(Player player, NpcHandle npc, DialogueSession session, ActionDef def);
}

