package dev.chrona.common.dialogue.actions;

import com.google.gson.JsonObject;
import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.dialogue.PlayerFlagStore;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;

public final class SetFlagAction implements DialogueAction {
    private final PlayerFlagStore store;

    public SetFlagAction(PlayerFlagStore store) {
        this.store = store;
    }

    @Override public String type() { return "set_flag"; }

    @Override
    public void execute(Player player, NpcHandle npc, DialogueSession session, ActionDef def) {
        JsonObject p = def.params();
        if (!p.has("key")) return;
        String key = p.get("key").getAsString();
        boolean value = !p.has("value") || p.get("value").getAsBoolean();
        store.setFlag(player.getUniqueId(), key, value);
        store.save();
    }
}
