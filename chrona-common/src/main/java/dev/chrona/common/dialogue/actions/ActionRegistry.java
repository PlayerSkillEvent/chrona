package dev.chrona.common.dialogue.actions;

import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActionRegistry {
    private final Logger log = ChronaLog.get(ActionRegistry.class);
    private final Map<String, DialogueAction> byType = new HashMap<>();

    public void register(DialogueAction action) {
        byType.put(action.type(), action);
    }

    public void executeAll(Player player, NpcHandle npc, DialogueSession session, List<ActionDef> defs) {
        if (defs == null || defs.isEmpty()) return;
        for (ActionDef def : defs) {
            DialogueAction a = byType.get(def.type());
            if (a == null) {
                log.warn("Unknown dialogue action type '{}'", def.type());
                continue;
            }
            a.execute(player, npc, session, def);
        }
    }
}

