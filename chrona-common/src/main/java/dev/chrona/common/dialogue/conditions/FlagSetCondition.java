package dev.chrona.common.dialogue.conditions;

import com.google.gson.JsonObject;
import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.dialogue.PlayerFlagStore;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public final class FlagSetCondition implements DialogueCondition {
    private final PlayerFlagStore store;
    private final boolean expected;

    public FlagSetCondition(PlayerFlagStore store, boolean expected) {
        this.store = store;
        this.expected = expected;
    }

    @Override public String type() {
        return expected ? "flag_set" : "flag_not_set";
    }

    @Override
    public boolean evaluate(Player player, NpcHandle npc, DialogueSession session, ConditionDef def) {
        JsonObject params = def.params();
        String key = params.has("key") ? params.get("key").getAsString() : null;
        if (key == null) return true;
        boolean has = store.hasFlag(player.getUniqueId(), key);
        return expected == has;
    }
}
