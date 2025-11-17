package dev.chrona.common.dialogue.conditions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConditionRegistry {
    private final Logger log = ChronaLog.get(ConditionRegistry.class);
    private final Map<String, DialogueCondition> byType = new HashMap<>();

    public void register(DialogueCondition condition) {
        byType.put(condition.type(), condition);
    }

    public boolean evaluateAll(Player player, NpcHandle npc, DialogueSession session, List<ConditionDef> defs) {
        if (defs == null || defs.isEmpty()) return true;
        for (ConditionDef def : defs) {
            DialogueCondition c = byType.get(def.type());
            if (c == null) {
                log.warn("Unknown dialogue condition type '{}'", def.type());
                continue;
            }
            if (!c.evaluate(player, npc, session, def)) return false;
        }
        return true;
    }
}

