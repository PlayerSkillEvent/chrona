package dev.chrona.common.npc.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.api.NpcClickListener;
import dev.chrona.common.npc.api.NpcHandle;
import dev.chrona.common.npc.api.NpcService;
import dev.chrona.common.npc.api.Skin;
import dev.chrona.common.npc.event.NpcInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolNpcs implements NpcService, Listener {
    private final Plugin plugin;
    private final NpcController controller;
    private final Logger logger;
    private final ProtocolManager pm;
    private final Map<UUID, PacketNpc> npcs = new ConcurrentHashMap<>();
    private final Set<NpcClickListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final PacketAdapter useListener;

    private final Set<PacketNpc> globalNpcs = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (var npc : globalNpcs) {
                if (npc.asHandle().location().getWorld().equals(p.getWorld()))
                    npc.asHandle().addViewer(p);
            }
        }, 5L);
    }

    public ProtocolNpcs(Plugin plugin, NpcController controller) {
        this.plugin = plugin;
        this.controller = controller;
        this.logger = ChronaLog.get(ProtocolNpcs.class);
        this.pm = ProtocolLibrary.getProtocolManager();

        this.useListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                var p = event.getPlayer();
                var packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);
                var actionStruct = packet.getEnumEntityUseActions().readSafely(0); // may be null on some versions
                NpcClickListener.ClickAction action = NpcClickListener.ClickAction.INTERACT;
                try {
                    var actionName = actionStruct.getAction().name();
                    if ("ATTACK".equals(actionName))
                        action = NpcClickListener.ClickAction.ATTACK;
                }
                catch (Throwable ignore) {}

                // entityId -> npc lookup
                PacketNpc hit = null;
                for (var npc : npcs.values())
                    if (npc.isEntityOf(p.getUniqueId(), entityId)) { hit = npc; break; }
                if (hit == null)
                    return;

                // notify listeners
                final NpcClickListener.ClickAction finalAction = action;
                var handle = hit.asHandle();
                listeners.forEach(l -> {
                    try {
                        l.onNpcInteract(p, handle, finalAction);
                    }
                    catch (Throwable t) {
                        logger.error("Error in NPC click listener", t);
                    }
                });
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.getPluginManager().callEvent(new NpcInteractEvent(p, handle, finalAction))
                );
            }
        };
        pm.addPacketListener(useListener);
    }

    public void clearAll() {
        npcs.values().forEach(PacketNpc::destroyAll);
        npcs.clear();
        pm.removePacketListener(useListener);
    }

    @Override
    public NpcHandle create(Location loc, String name, Skin skin) {
        var npc = new PacketNpc(plugin, pm, controller, loc, name, skin);
        npcs.put(npc.id(), npc);
        globalNpcs.add(npc);
        return npc.asHandle();
    }

    @Override
    public NpcHandle createFor(Player viewer, Location loc, String name, Skin skin) {
        var npc = new PacketNpc(plugin, pm, controller, loc, name, skin);
        npcs.put(npc.id(), npc);
        npc.asHandle().addViewer(viewer);
        return npc.asHandle();
    }

    @Override
    public void registerListener(NpcClickListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(NpcClickListener listener) {
        listeners.remove(listener);
    }
}

