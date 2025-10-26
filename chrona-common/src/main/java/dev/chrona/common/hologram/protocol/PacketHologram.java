package dev.chrona.common.hologram.protocol;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.chrona.common.hologram.api.HologramHandle;
import dev.chrona.common.log.ChronaLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class PacketHologram implements HologramHandle {
    private static final AtomicInteger ENTITY_ID = new AtomicInteger(200000); // sicherer Bereich

    private final UUID id = UUID.randomUUID();
    private volatile Location loc;
    private volatile List<String> lines;

    private final Logger logger;
    private final ProtocolManager pm;

    /** Per-Viewer eigene Entity-IDs (eine pro Zeile) */
    private final Map<UUID, int[]> viewerEntities = new ConcurrentHashMap<>();

    PacketHologram(ProtocolManager pm, Location loc, List<String> lines) {
        this.logger = ChronaLog.get(PacketHologram.class);
        this.pm = pm;
        this.loc = loc.clone();
        this.lines = new ArrayList<>(lines);
    }

    // ---------- HologramHandle impl ----------

    @Override public UUID id() {
        return id;
    }

    @Override public Location location() {
        return loc.clone();
    }

    @Override
    public synchronized void setLines(List<String> newLines) {
        this.lines = new ArrayList<>(newLines);
        // simplest & safest: destroy+spawn neu für alle Viewer
        for (UUID v : viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null && p.isOnline()) {
                destroyFor(p);
                spawnFor(p);
            }
        }
    }

    @Override
    public void setLine(int i, String text) {
        if (i < 0) return;
        final String newText = text == null ? "" : text;
        synchronized (this) {
            if (i >= lines.size()) return;
            lines.set(i, newText);
        }
        for (UUID v : viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null || !p.isOnline()) continue;
            int[] ids = viewerEntities.get(v);
            if (ids == null || i >= ids.length) continue;
            try {
                sendMetadata(p, ids[i], newText);
            }
            catch (Exception e) {
                logger.warn("Hologram setLine failed for {}", p.getName(), e);
            }
        }
    }

    @Override
    public synchronized void addViewer(Player p) {
        if (p == null || !p.isOnline()) return;
        if (viewerEntities.containsKey(p.getUniqueId())) return; // schon aktiv
        spawnFor(p);
    }

    @Override
    public synchronized void removeViewer(Player p) {
        if (p == null) return;
        destroyFor(p);
    }

    @Override
    public synchronized Set<UUID> viewers() {
        return new HashSet<>(viewerEntities.keySet());
    }

    @Override
    public void setVisible(boolean v) {

    }

    @Override
    public synchronized void teleport(Location to) {
        this.loc = to.clone();
        for (UUID v : viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null && p.isOnline()) {
                destroyFor(p);
                spawnFor(p);
            }
        }
    }

    @Override
    public synchronized void destroy() {
        for (UUID v : viewers()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null && p.isOnline()) destroyFor(p);
        }
        viewerEntities.clear();
    }

    @Override
    public Display getEntity() {
        return null;
    }

    // ---------- Packet-Lowlevel ----------

    private void spawnFor(Player viewer) {
        int count = Math.max(1, lines.size());
        int base = ENTITY_ID.getAndAdd(count);
        int[] ids = new int[count];

        double y = 0;
        for (int i = 0; i < count; i++) {
            int entId = base + i;
            ids[i] = entId;
            Location l = loc.clone().add(0.5, 1.35 + y, 0.5);

            sendSpawnArmorStand(viewer, entId, l);
            sendMetadata(viewer, entId, lines.get(i));

            y += 0.25;
        }
        viewerEntities.put(viewer.getUniqueId(), ids);
    }

    private void destroyFor(Player viewer) {
        int[] ids = viewerEntities.remove(viewer.getUniqueId());

        if (ids == null || ids.length == 0)
            return;

        try {
            var destroy = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, java.util.Arrays.stream(ids).boxed().toList());
            pm.sendServerPacket(viewer, destroy);
        }
        catch (Exception e) {
            logger.warn("Hologram destroy failed for {}", viewer.getName(), e);
        }
    }

    private void sendSpawnArmorStand(Player viewer, int entityId, Location l) {
        try {
            var spawn = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, entityId);
            spawn.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
            spawn.getUUIDs().write(0, UUID.randomUUID());
            spawn.getDoubles().write(0, l.getX());
            spawn.getDoubles().write(1, l.getY());
            spawn.getDoubles().write(2, l.getZ());
            pm.sendServerPacket(viewer, spawn);
        }
        catch (Exception e) {
            logger.error("Hologram spawn failed", e);
        }
    }

    /** 1.19.3+ Weg: WrappedDataValue-Liste */
    private void sendMetadata(Player viewer, int entityId, String textLine) {
        try {
            var meta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            meta.getIntegers().write(0, entityId);

            var BYTE = WrappedDataWatcher.Registry.get((Type) Byte.class);
            var BOOL = WrappedDataWatcher.Registry.get((Type) Boolean.class);
            var CHAT = WrappedDataWatcher.Registry.getChatComponentSerializer(true);

            var values = new ArrayList<WrappedDataValue>();
            // 0: Entity Flags → invisible (0x20)
            values.add(new WrappedDataValue(0, BYTE, (byte) 0x20));
            // 2: Custom Name (Optional<ChatComponent>)
            var chat = WrappedChatComponent.fromText(textLine).getHandle();
            values.add(new WrappedDataValue(2, CHAT, java.util.Optional.of(chat)));
            // 3: Custom Name Visible (boolean)
            values.add(new WrappedDataValue(3, BOOL, true));
            // 15: ArmorStand flags (Marker/Small/etc.) – Marker (0x10) + Small (0x01)
            byte armorStandFlags = (byte) (0x10 | 0x01);
            values.add(new WrappedDataValue(15, BYTE, armorStandFlags));

            meta.getDataValueCollectionModifier().write(0, values);
            pm.sendServerPacket(viewer, meta);
        } catch (Exception e) {
            logger.error("Hologram metadata failed", e);
        }
    }
}

