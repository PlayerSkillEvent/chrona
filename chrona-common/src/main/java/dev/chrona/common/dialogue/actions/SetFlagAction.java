package dev.chrona.common.dialogue.actions;

import com.google.gson.JsonObject;
import dev.chrona.common.dialogue.DialogueSession;
import dev.chrona.common.dialogue.FlagMetadata;
import dev.chrona.common.dialogue.PlayerFlagStore;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SetFlagAction implements DialogueAction {
    private final PlayerFlagStore store;

    public SetFlagAction(PlayerFlagStore store) {
        this.store = store;
    }

    @Override
    public String type() {
        return "set_flag";
    }

    @Override
    public void execute(Player player, NpcHandle npc, DialogueSession session, ActionDef def) {
        JsonObject params = def.params();
        if (!params.has("key"))
            return;

        String key = params.get("key").getAsString();
        boolean value = !params.has("value") || params.get("value").getAsBoolean();

        String dialogueId = session != null ? session.getDialogueId() : null;
        String nodeId     = session != null ? session.getCurrentNodeId() : null;
        String sessionId  = session != null ? session.getSessionId() : null;

        String npcId      = npc != null && npc.id() != null ? npc.id().toString() : null;
        String npcName    = npc != null ? npc.name() : null;

        String source = dialogueId != null
                ? "dialogue:" + dialogueId
                : "dialogue";

        FlagMetadata meta = FlagMetadata.builder()
                .source(source)
                .putExtra("dialogueId", dialogueId)
                .putExtra("nodeId", nodeId)
                .putExtra("sessionId", sessionId)
                .putExtra("npcId", npcId)
                .putExtra("npcName", npcName)
                .putExtra("actionType", type())
                .putExtra("flagKey", key)
                .build();

        if (player != null)
            store.setFlag(player.getUniqueId(), key, value, meta);
    }
}
