package dev.chrona.common.dialogue.conditions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public interface DialogueCondition {
    String type();
    boolean evaluate(Player player, NpcHandle npc, DialogueSession session, ConditionDef def);
}

