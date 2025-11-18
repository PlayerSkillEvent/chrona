package dev.chrona.common.dialogue.conditions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public interface DialogueCondition {

    /** Returns the type identifier of this condition. */
    String type();

    /** Evaluates the condition with the given parameters. */
    boolean evaluate(Player player, NpcHandle npc, DialogueSession session, ConditionDef def);
}

