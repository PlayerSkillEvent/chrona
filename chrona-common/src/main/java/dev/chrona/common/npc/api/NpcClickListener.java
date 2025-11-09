package dev.chrona.common.npc.api;

import org.bukkit.entity.Player;

public interface NpcClickListener {
    void onNpcInteract(Player player, NpcHandle npc, ClickAction action);

    enum ClickAction { INTERACT, ATTACK } // RIGHT/LEFT
}

