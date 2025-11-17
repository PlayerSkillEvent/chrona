package dev.chrona.plugin.listeners;

import dev.chrona.common.dialogue.DialogueService;
import dev.chrona.common.npc.event.NpcInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DialogueListener implements Listener {
    private final DialogueService dialogue;

    public DialogueListener(DialogueService dialogue) {
        this.dialogue = dialogue;
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        if (event.getAction() != dev.chrona.common.npc.api.NpcClickListener.ClickAction.INTERACT)
            return;

        Player player = event.getPlayer();
        dialogue.handleNpcInteract(player, event.getNpc());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dialogue.endSessionFor(event.getPlayer());
    }
}

