package dev.chrona.common.npc.event;

import dev.chrona.common.npc.api.NpcClickListener;
import dev.chrona.common.npc.api.NpcHandle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class NpcInteractEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final NpcHandle npc;
    private final NpcClickListener.ClickAction action;

    public NpcInteractEvent(Player player, NpcHandle npc, NpcClickListener.ClickAction action) {
        super(false);
        this.player = player; this.npc = npc; this.action = action;
    }

    public Player getPlayer() { return player; }
    public NpcHandle getNpc() { return npc; }
    public NpcClickListener.ClickAction getAction() { return action; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

