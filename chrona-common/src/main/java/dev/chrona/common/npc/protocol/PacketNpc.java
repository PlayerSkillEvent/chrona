package dev.chrona.common.npc.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.wrappers.*;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.api.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PacketNpc {
    private static final AtomicInteger ENTITY_ID = new AtomicInteger(300000);
    private static final double LOOK_RADIUS_SQ = 12*12;

    private static final double TICK_DT = 0.05;
    private static final double STEP_HEIGHT = 0.6;
    private static final double JUMP_HEIGHT = 1.1;
    private static final double GRAVITY = 18.0;
    private static final double GROUND_SNAP = 0.02;
    private static final double MAX_REL_STEP = 8.0;

    // --------- Head-State: Anti-Jitter ----------
    private enum HeadMode { PATH, PLAYER }
    private HeadMode headMode = HeadMode.PATH;
    private UUID trackedPlayerId = null;
    private long headHoldUntilMs = 0L;
    private float headYaw = 0f, headPitch = 0f;
    private float lastSentHeadYaw = Float.NaN;

    private static final double ENTER_R = 5.0;
    private static final double EXIT_R  = 6.5;
    private static final float  MAX_REL_HEAD_YAW = 65f;
    private static final float  YAW_SPEED_DEG_TICK = 6f;
    private static final float  PITCH_SPEED_DEG_TICK = 4f;
    private static final long   MIN_HOLD_MS = 400L;
    private static final float  EPS_HEAD_SEND = 0.5f;
    private static final float BODY_YAW_SPEED_DEG_TICK = 8f;

    private final UUID id = UUID.randomUUID();
    private final UUID profileUuid;
    private volatile String name;
    private volatile Skin skin;
    private volatile Location loc;
    private volatile RotationSource rotationSource = null;
    private volatile PathRunner pathRunner;

    private final Plugin plugin;
    private final Logger logger;
    private final ProtocolManager pm;
    private final NpcController controller;

    private final NpcController.LookMode lookMode = NpcController.LookMode.FIXED;
    private String currentPathName;

    /** pro Viewer: entityId + TabShownFlag */
    private final Map<UUID, ViewerState> viewers = new ConcurrentHashMap<>();

    PacketNpc(Plugin plugin, ProtocolManager pm, NpcController controller, Location loc, String name, Skin skin) {
        this.plugin = plugin;
        this.logger = ChronaLog.get(PacketNpc.class);
        this.pm = pm;
        this.controller = controller;
        this.loc = loc.clone();
        this.name = name;
        this.skin = skin;
        this.profileUuid = UUID.randomUUID();
        this.headYaw = loc.getYaw();
        this.headPitch = loc.getPitch();
    }

    UUID id() {
        return id;
    }

    boolean isEntityOf(UUID viewer, int entityId) {
        var vs = viewers.get(viewer);
        return vs != null && vs.entityId == entityId;
    }

    NpcHandle asHandle() {
        PacketNpc self = this;
        return new NpcHandle() {
            @Override public UUID id() { return self.id; }
            @Override public UUID uuid() { return self.profileUuid; }
            @Override public Skin skin() { return self.skin; }
            @Override public String name() { return self.name; }
            @Override public Location location() { return self.loc.clone(); }
            @Override public void addViewer(Player p) { self.spawnFor(p); }
            @Override public void removeViewer(Player p) { self.destroyFor(p); }
            @Override public Set<UUID> viewers() { return new HashSet<>(self.viewers.keySet()); }
            @Override public void setName(String newName) { self.setName(newName); }
            @Override public void setSkin(Skin s) { self.setSkin(s); }
            @Override public void setEquipment(EquipmentSlot slot, ItemStack item) { self.setEquipment(slot, item); }
            @Override public void setRotationSource(RotationSource source) { self.setRotationSource(source); }
            @Override public void teleport(Location to) { self.teleport(to); }
            @Override public void lookAt(Player viewer, Location target) { self.lookAt(viewer, target); }
            @Override public void runPath(Path path) { self.runPath(path); }
            @Override public void stopPath() { self.stopPath(); }
            @Override public void pausePath(boolean pause) { self.pausePath(pause); }
            @Override public void destroy() { self.destroyAll(); }
            @Override public NpcPersistence.NpcRuntime state() { return self.snapshot(); }
            @Override public void resumePath(Path path, int index, int dir, long waitMs) { self.resumePath(path, index, dir, waitMs); }
        };
    }

    private void setName(String newName) {
        this.name = newName;
        // Für alle Viewer: Tab remove + readd + metadata name
        for (UUID v : viewers.keySet()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null || !p.isOnline())
                continue;
            destroyFor(p);
            spawnFor(p);
        }
    }

    private void setSkin(Skin s) {
        this.skin = s;
        // Neuspawn pro Viewer (Skin steckt im GameProfile)
        for (UUID v : viewers.keySet()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null || !p.isOnline())
                continue;
            destroyFor(p);
            spawnFor(p);
        }
        controller.onSkinSet(asHandle(), s);
    }

    private void setEquipment(NpcHandle.EquipmentSlot slot, ItemStack item) {
        for (UUID v : viewers.keySet()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null || !p.isOnline()) continue;
            sendEquipment(p, Map.of(slot, item));
        }
    }

    private void teleport(Location to) {
        this.loc = to.clone();
        this.headYaw = to.getYaw();
        this.headPitch = to.getPitch();
        this.lastSentHeadYaw = Float.NaN;
        for (UUID v : viewers.keySet()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null || !p.isOnline())
                continue;
            sendTeleport(p);
        }
    }

    private void lookAt(Player viewer, Location target) {
        var vs = viewers.get(viewer.getUniqueId());
        if (vs == null)
            return;
        sendLook(viewer, vs.entityId, target);
    }

    void destroyAll() {
        for (UUID v : viewers.keySet()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null && p.isOnline())
                destroyFor(p);
        }
        viewers.clear();
    }

    public void setRotationSource(RotationSource src) {
        this.rotationSource = src;
    }

    public synchronized void runPath(Path path) {
        stopPath();
        this.currentPathName = path.name;
        this.pathRunner = new PathRunner(path);
        controller.onPathStarted(asHandle(), path.name, path);
        this.pathRunner.start();
    }

    public synchronized void stopPath() {
        if (pathRunner != null) {
            pathRunner.stop();
            pathRunner = null;
            controller.onPathStopped(asHandle());
            headMode = HeadMode.PATH;
            trackedPlayerId = null;
            headHoldUntilMs = 0L;
        }
    }

    public synchronized void pausePath(boolean pause) {
        if (pathRunner != null)
            pathRunner.setPaused(pause);
    }

    private void resumePath(Path p, int index, int dir, long waitMs) {
        stopPath();
        this.pathRunner = new PathRunner(p);
        this.pathRunner.idx = Math.min(Math.max(0, index), p.points.size()-1);
        this.pathRunner.dir = (dir == -1 ? -1 : 1);
        if (waitMs > 0)
            this.pathRunner.waitUntil = System.currentTimeMillis() + waitMs;
        this.pathRunner.start();
    }

    public NpcPersistence.NpcRuntime snapshot() {
        var rt = new NpcPersistence.NpcRuntime(asHandle());
        rt.internalId = this.id;
        rt.skin = this.skin;
        rt.lookMode = this.lookMode;
        rt.lookRadius = 12.0;

        var l = this.loc.clone();
        rt.npc.teleport(l);

        if (this.pathRunner != null) {
            rt.pathRunning = true;
            rt.pathName = this.currentPathName;
            rt.pathSpeed = this.pathRunner.path.speedBlocksPerSec;
            rt.pathLoop = this.pathRunner.path.loop;
            rt.pathIndex = this.pathRunner.idx;
            rt.pathDir = this.pathRunner.dir;

            long now = System.currentTimeMillis();
            rt.waitRemainingMs = Math.max(0, this.pathRunner.waitUntil - now);
        }
        return rt;
    }

    // ---------- Spawning per Viewer ----------

    private void spawnFor(Player viewer) {
        if (viewer == null || !viewer.isOnline() || viewers.containsKey(viewer.getUniqueId()))
            return;

        int entityId = ENTITY_ID.getAndIncrement();
        var vs = new ViewerState(entityId);
        viewers.put(viewer.getUniqueId(), vs);

        sendPlayerInfoAdd(viewer);
        sendSpawnPlayer(viewer, entityId);
        sendMetadata(viewer, entityId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendPlayerInfoRemove(viewer), 10L);

        vs.startTracking(plugin, () -> trackLook(viewer, vs));
    }

    private void destroyFor(Player viewer) {
        var vs = viewers.remove(viewer.getUniqueId());
        if (vs == null)
            return;
        vs.stopTracking();
        try {
            var destroy = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, java.util.List.of(vs.entityId));
            pm.sendServerPacket(viewer, destroy);
            sendPlayerInfoRemove(viewer);
        } catch (Exception e) {
            logger.warn("NPC destroy failed", e);
        }
    }

    private void trackLook(Player viewer, ViewerState vs) {
        if (pathRunner != null)
            return;
        if (viewer == null || !viewer.isOnline())
            return;
        if (viewer.getWorld() != loc.getWorld())
            return;
        if (viewer.getLocation().distanceSquared(loc) > LOOK_RADIUS_SQ)
            return;

        float bodyYaw = loc.getYaw();
        float[] hp = updateHead(false, bodyYaw);
        float newBodyYaw = approachAngle(bodyYaw, hp[0], BODY_YAW_SPEED_DEG_TICK);
        boolean bodyChanged = Math.abs(normYaw(newBodyYaw - bodyYaw)) > EPS_HEAD_SEND;
        boolean headChanged = Float.isNaN(lastSentHeadYaw) || Math.abs(normYaw(hp[0] - lastSentHeadYaw)) > EPS_HEAD_SEND;

        if (bodyChanged)
            loc.setYaw(newBodyYaw);

        for (UUID v : viewers.keySet()) {
            Player p = Bukkit.getPlayer(v);
            if (p == null || !p.isOnline())
                continue;
            ViewerState vstate = viewers.get(v);
            if (vstate == null)
                continue;

            if (bodyChanged)
                sendEntityLookOnly(p, vstate.entityId, newBodyYaw, hp[1], true);
            if (headChanged)
                sendHeadRotationOnly(p, vstate.entityId, hp[0]);

        }
        if (headChanged) lastSentHeadYaw = hp[0];
    }

    // ---------- Packets ----------

    private void sendPlayerInfoAdd(Player viewer) {
        try {
            var prof = new WrappedGameProfile(profileUuid, trimName(name));
            prof.getProperties().clear();
            if (skin != null && skin.value() != null && skin.signature() != null) {
                prof.getProperties().put("textures",
                        new WrappedSignedProperty("textures", skin.value(), skin.signature()));
            }

            var pkt = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
            pkt.getModifier().writeDefaults();

            var actions = EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
                    EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
                    EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
            );

            var data = new PlayerInfoData(
                    prof,
                    0,
                    EnumWrappers.NativeGameMode.CREATIVE,
                    WrappedChatComponent.fromText(name)
            );

            pkt.getPlayerInfoActions().write(0, actions);
            pkt.getPlayerInfoDataLists().write(1, List.of(data));

            pm.sendServerPacket(viewer, pkt);
        } catch (Exception e) {
            logger.error("PLAYER_INFO add failed", e);
        }
    }

    private void sendPlayerInfoRemove(Player viewer) {
        try {
            var rem = pm.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            rem.getUUIDLists().write(0, java.util.List.of(profileUuid));
            pm.sendServerPacket(viewer, rem);
        }
        catch (Exception e) {
            logger.error("PLAYER_INFO_REMOVE failed", e);
        }
    }

    private void sendSpawnPlayer(Player viewer, int entityId) {
        try {
            var spawn = pm.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, entityId).writeSafely(1, entityId);
            spawn.getUUIDs().write(0, profileUuid);
            spawn.getEntityTypeModifier().writeSafely(0, EntityType.PLAYER);
            spawn.getDoubles().write(0, loc.getX());
            spawn.getDoubles().write(1, loc.getY());
            spawn.getDoubles().write(2, loc.getZ());

            byte yaw = toAngleByte(loc.getYaw());
            byte pitch = toAngleByte(loc.getPitch());
            spawn.getBytes().write(0, yaw);   // yaw
            spawn.getBytes().write(1, pitch); // pitch

            pm.sendServerPacket(viewer, spawn);
        } catch (Exception e) {
            plugin.getLogger().severe("[PacketNpc] SPAWN_PLAYER failed: " + e.getMessage());
        }
    }

    private void sendMetadata(Player viewer, int entityId) {
        try {
            var meta = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            meta.getIntegers().write(0, entityId);

            var BYTE = WrappedDataWatcher.Registry.get((Type) Byte.class);
            var BOOL = WrappedDataWatcher.Registry.get((Type) Boolean.class);
            var CHAT = WrappedDataWatcher.Registry.getChatComponentSerializer(true);

            var values = new ArrayList<WrappedDataValue>();
            values.add(new WrappedDataValue(0, BYTE, (byte) 0x00));
            values.add(new WrappedDataValue(2, CHAT,
                    Optional.of(WrappedChatComponent.fromText(name).getHandle())));
            values.add(new WrappedDataValue(3, BOOL, true));
            values.add(new WrappedDataValue(17, BYTE, (byte) 0x7F)); // alles an

            meta.getDataValueCollectionModifier().write(0, values);
            pm.sendServerPacket(viewer, meta);
        } catch (Exception e) {
            logger.error("ENTITY_METADATA failed", e);
        }
    }

    private void sendLook(Player viewer, int entityId, Location playerLoc) {
        try {
            float[] yp = computeYawPitch(this.loc, playerLoc, viewer.getEyeHeight());
            byte yawB = toAngleByte(yp[0]);
            byte pitchB = toAngleByte(yp[1]);

            var head = pm.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            head.getIntegers().write(0, entityId);
            head.getBytes().write(0, yawB);

            var look = pm.createPacket(PacketType.Play.Server.ENTITY_LOOK);
            look.getIntegers().write(0, entityId);
            look.getBytes().write(0, yawB);
            look.getBytes().write(1, pitchB);
            look.getBooleans().write(0, true);

            pm.sendServerPacket(viewer, head);
            pm.sendServerPacket(viewer, look);
        } catch (Exception e) {
            logger.error("ENTITY_LOOK failed", e);
        }
    }

    private void sendHeadRotationOnly(Player viewer, int entityId, float yaw) {
        try {
            var head = pm.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            head.getIntegers().write(0, entityId);
            head.getBytes().write(0, toAngleByte(yaw));
            pm.sendServerPacket(viewer, head);
        } catch (Exception e) {
            logger.error("HEAD_ROTATION failed", e);
        }
    }

    private void sendEntityLookOnly(Player viewer, int entityId, float yaw, float pitch, boolean onGround) {
        try {
            var look = pm.createPacket(PacketType.Play.Server.ENTITY_LOOK);
            look.getIntegers().write(0, entityId);
            look.getBytes().write(0, toAngleByte(yaw));
            look.getBytes().write(1, toAngleByte(pitch));
            look.getBooleans().write(0, onGround);
            pm.sendServerPacket(viewer, look);
        } catch (Exception e) {
            logger.error("ENTITY_LOOK (only) failed", e);
        }
    }

    private void sendEquipment(Player viewer, Map<NpcHandle.EquipmentSlot, ItemStack> items) {
        var vs = viewers.get(viewer.getUniqueId());
        if (vs == null) return;
        try {
            var pkt = pm.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            pkt.getIntegers().write(0, vs.entityId);
            var pairList = new ArrayList<Pair<EnumWrappers.ItemSlot, ItemStack>>();
            for (var e : items.entrySet()) {
                var slot = switch (e.getKey()) {
                    case MAIN_HAND -> EnumWrappers.ItemSlot.MAINHAND;
                    case OFF_HAND  -> EnumWrappers.ItemSlot.OFFHAND;
                    case HEAD      -> EnumWrappers.ItemSlot.HEAD;
                    case CHEST     -> EnumWrappers.ItemSlot.CHEST;
                    case LEGS      -> EnumWrappers.ItemSlot.LEGS;
                    case FEET      -> EnumWrappers.ItemSlot.FEET;
                };
                pairList.add(new Pair<>(slot, e.getValue()));
            }
            pkt.getSlotStackPairLists().write(0, pairList);
            pm.sendServerPacket(viewer, pkt);
        } catch (Exception e) {
            logger.error("ENTITY_EQUIPMENT failed", e);
        }
    }

    private void sendTeleport(Player viewer) {
        sendTeleport(viewer, this.loc);
    }

    private void sendTeleport(Player viewer, Location to) {
        var vs = viewers.get(viewer.getUniqueId());
        if (vs == null) return;
        try {
            var tp = pm.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            tp.getIntegers().write(0, vs.entityId);
            tp.getDoubles().write(0, to.getX());
            tp.getDoubles().write(1, to.getY());
            tp.getDoubles().write(2, to.getZ());
            tp.getBytes().write(0, toAngleByte(to.getYaw()));
            tp.getBytes().write(1, toAngleByte(to.getPitch()));
            tp.getBooleans().write(0, true); // onGround
            pm.sendServerPacket(viewer, tp);
        } catch (Exception e) {
            logger.error("ENTITY_TELEPORT failed", e);
        }
    }

    private void sendRelMoveLook(Player viewer, int entityId, double dx, double dy, double dz, float yaw, float pitch, boolean onGround) {
        try {
            int x = (int) Math.round(dx * 4096.0);
            int y = (int) Math.round(dy * 4096.0);
            int z = (int) Math.round(dz * 4096.0);

            var pkt = pm.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
            pkt.getIntegers().write(0, entityId);
            pkt.getShorts().write(0, (short) x);
            pkt.getShorts().write(1, (short) y);
            pkt.getShorts().write(2, (short) z);
            pkt.getBytes().write(0, toAngleByte(yaw));
            pkt.getBytes().write(1, toAngleByte(pitch));
            pkt.getBooleans().write(0, onGround);

            pm.sendServerPacket(viewer, pkt);
        } catch (Exception e) {
            logger.error("REL_ENTITY_MOVE_LOOK failed", e);
        }
    }

    private static byte toAngleByte(float deg) {
        float norm = (deg % 360f + 360f) % 360f;
        return (byte) (int) Math.floor(norm * 256f / 360f);
    }

    private static float[] computeYawPitch(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = (to.getY() + 1.62) - (from.getY() + 1.62);
        double dz = to.getZ() - from.getZ();
        double distXZ = Math.max(1e-6, Math.sqrt(dx*dx + dz*dz));
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));
        if (pitch > 90) pitch = 90;
        if (pitch < -90) pitch = -90;
        return new float[]{yaw, pitch};
    }

    private static float[] computeYawPitch(Location from, Location to, double toEye) {
        double fx = from.getX();
        double fy = from.getY() + 1.62; // FIX: statt 3.24
        double fz = from.getZ();
        double tx = to.getX();
        double ty = to.getY() + toEye;
        double tz = to.getZ();

        double dx = tx - fx;
        double dy = ty - fy;
        double dz = tz - fz;

        double distXZ = Math.max(1e-6, Math.sqrt(dx*dx + dz*dz));
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));
        if (pitch > 90) pitch = 90;
        if (pitch < -90) pitch = -90;
        return new float[]{yaw, pitch};
    }

    private static String trimName(String n) {
        if (n == null) return "ChronaNPC";
        return n.length() <= 16 ? n : n.substring(0, 16);
    }

    private static float normYaw(float deg) {
        float d = deg % 360f;
        if (d <= -180f) d += 360f;
        if (d > 180f) d -= 360f;
        return d;
    }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float approachAngle(float current, float target, float maxStep) {
        float delta = normYaw(target - current);
        if (Math.abs(delta) <= maxStep) return target;
        return current + Math.copySign(maxStep, delta);
    }

    private Player findNearestPlayerWithin(Location from, double enterR, double exitR) {
        double r = (headMode == HeadMode.PATH) ? enterR : exitR;
        double r2 = r * r;
        Player best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Player p : from.getWorld().getPlayers()) {
            if (p.isDead() || !p.isValid()) continue;
            double d2 = p.getLocation().distanceSquared(from);
            if (d2 <= r2 && d2 < bestD2) { best = p; bestD2 = d2; }
        }
        return best;
    }

    /** Updatet global den Head-Zielwinkel mit Hysterese + Smooth. */
    private float[] updateHead(boolean moving, float bodyYaw) {
        long now = System.currentTimeMillis();
        Player nearest = findNearestPlayerWithin(loc, ENTER_R, EXIT_R);

        switch (headMode) {
            case PATH -> {
                if (nearest != null) {
                    headMode = HeadMode.PLAYER;
                    trackedPlayerId = nearest.getUniqueId();
                    headHoldUntilMs = now + MIN_HOLD_MS;
                }
            }
            case PLAYER -> {
                Player tracked = (trackedPlayerId != null) ? Bukkit.getPlayer(trackedPlayerId) : null;
                boolean lost =
                        tracked == null ||
                                !tracked.isValid() ||
                                tracked.isDead() ||
                                tracked.getWorld() != loc.getWorld() ||
                                tracked.getLocation().distanceSquared(loc) > (EXIT_R * EXIT_R);
                if (lost && now >= headHoldUntilMs) {
                    headMode = HeadMode.PATH;
                    trackedPlayerId = null;
                    headHoldUntilMs = now + MIN_HOLD_MS;
                }
            }
        }

        float targetHeadYaw;
        float targetHeadPitch;

        if (headMode == HeadMode.PLAYER) {
            Player tracked = (trackedPlayerId != null) ? Bukkit.getPlayer(trackedPlayerId) : null;
            if (tracked != null && tracked.isValid() && tracked.getWorld() == loc.getWorld()) {
                Location eyeNpc = loc.clone().add(0, 1.62, 0);
                Location eyePl  = tracked.getLocation().clone().add(0, tracked.getEyeHeight(), 0);

                double dx = eyePl.getX() - eyeNpc.getX();
                double dz = eyePl.getZ() - eyeNpc.getZ();
                double dy = eyePl.getY() - eyeNpc.getY();

                float yawToPl   = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitchToPl = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));

                if (moving) {
                    float rel = normYaw(yawToPl - bodyYaw);
                    rel = clamp(rel, -MAX_REL_HEAD_YAW, MAX_REL_HEAD_YAW);
                    targetHeadYaw = bodyYaw + rel;
                    targetHeadPitch = 0f; // beim Laufen kein Pitch
                } else {
                    targetHeadYaw = yawToPl;
                    targetHeadPitch = pitchToPl;
                }
            } else {
                targetHeadYaw = bodyYaw;
                targetHeadPitch = 0f;
            }
        } else {
            targetHeadYaw = bodyYaw;
            targetHeadPitch = 0f;
        }

        headYaw   = approachAngle(headYaw,   targetHeadYaw,   YAW_SPEED_DEG_TICK);
        headPitch = approachAngle(headPitch, targetHeadPitch, PITCH_SPEED_DEG_TICK);
        return new float[]{headYaw, headPitch};
    }

    private static final class ViewerState {
        final int entityId;
        private int taskId = -1;
        ViewerState(int entityId) { this.entityId = entityId; }
        void startTracking(Plugin plugin, Runnable task) {
            stopTracking();
            this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 2L, 2L);
        }
        void stopTracking() {
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
            }
        }
    }

    public interface RotationSource { Location targetOrNull(); }

    private NpcPersistence.NpcRuntime toRuntime() {
        var rt = new NpcPersistence.NpcRuntime(asHandle());
        rt.internalId = this.id;
        rt.skin = this.skin;
        if (pathRunner != null) {
            rt.pathRunning = true;
            rt.pathName = currentPathName;
            rt.pathIndex = pathRunner.idx;
            rt.pathDir = pathRunner.dir;
            long now = System.currentTimeMillis();
            rt.waitRemainingMs = Math.max(0, pathRunner.waitUntil - now);
            rt.pathSpeed = pathRunner.path.speedBlocksPerSec;
            rt.pathLoop = pathRunner.path.loop;
        }
        return rt;
    }

    private final class PathRunner implements Runnable {
        private final Path path;
        private int taskId = -1;
        private int idx = 0;
        private int dir = 1;
        private long waitUntil = 0L;
        private volatile boolean paused = false;
        private double vy = 0.0;
        private boolean airborne = false;

        PathRunner(Path path) { this.path = path; }

        void start() { taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 2L, 1L); }
        void stop()  { if (taskId != -1) { Bukkit.getScheduler().cancelTask(taskId); taskId = -1; } }
        void setPaused(boolean p) { paused = p; }

        @Override
        public void run() {
            if (paused) return;
            if (viewers.isEmpty()) return;

            final Waypoint targetWp = path.points.get(idx);
            final Location from = loc.clone();
            final Location to = targetWp.loc;

            final long now = System.currentTimeMillis();

            if (waitUntil > now) {
                float bodyYaw = loc.getYaw();
                float[] hp = updateHead(false, bodyYaw);
                float newBodyYaw = approachAngle(bodyYaw, hp[0], BODY_YAW_SPEED_DEG_TICK);
                boolean bodyChanged = Math.abs(normYaw(newBodyYaw - bodyYaw)) > EPS_HEAD_SEND;

                if (bodyChanged)
                    loc.setYaw(newBodyYaw);

                for (UUID v : viewers.keySet()) {
                    Player p = Bukkit.getPlayer(v);
                    if (p == null || !p.isOnline())
                        continue;
                    ViewerState vs = viewers.get(v);
                    if (vs == null)
                        continue;

                    if (bodyChanged)
                        sendEntityLookOnly(p, vs.entityId, newBodyYaw, hp[1], true);
                    sendHeadRotationOnly(p, vs.entityId, hp[0]);
                }
                return;
            }

            // ----------- Bewegungslogik -----------
            double dxWp = to.getX() - from.getX();
            double dzWp = to.getZ() - from.getZ();
            double dyWp = to.getY() - from.getY();

            final double REACH_R = 0.35;
            final double REACH_Y = 1.25;

            boolean reached = (Math.hypot(dxWp, dzWp) <= REACH_R) && (Math.abs(dyWp) <= REACH_Y);
            if (reached) {
                loc.setX(to.getX());
                loc.setZ(to.getZ());
                waitUntil = targetWp.waitMs > 0 ? now + targetWp.waitMs : 0L;
                advanceIndex();
                return;
            }

            double toDx = to.getX() - from.getX();
            double toDz = to.getZ() - from.getZ();
            double distXZ = Math.hypot(toDx, toDz);
            double step = path.speedBlocksPerSec * TICK_DT;
            double moveXZ = Math.min(step, Math.max(1e-6, distXZ));
            double mx = (toDx / Math.max(1e-6, distXZ)) * moveXZ;
            double mz = (toDz / Math.max(1e-6, distXZ)) * moveXZ;

            double baseTargetY = findSurfaceY(from.getWorld(), from.getX() + mx, from.getY(), from.getZ() + mz, 6, 6);
            double desiredY = baseTargetY + GROUND_SNAP;
            double dyTarget = desiredY - from.getY();

            double nx, ny, nz;
            boolean onGround;

            if (!airborne && dyTarget > 0 && dyTarget <= STEP_HEIGHT) {
                double dy = Math.min(dyTarget, step * 0.8);
                vy = 0;
                nx = from.getX() + mx;
                ny = from.getY() + dy;
                nz = from.getZ() + mz;
                airborne = false;
                onGround = true;
            }
            else if (!airborne && dyTarget > STEP_HEIGHT && dyTarget <= JUMP_HEIGHT) {
                airborne = true;
                vy = Math.sqrt(2 * GRAVITY * dyTarget);
                nx = from.getX() + mx;
                nz = from.getZ() + mz;
                ny = from.getY() + vy * TICK_DT * 0.5;
                onGround = false;
            }
            else {
                nx = from.getX() + mx;
                nz = from.getZ() + mz;

                if (airborne) {
                    vy -= GRAVITY * TICK_DT;
                    ny = from.getY() + vy * TICK_DT;
                    if (ny <= desiredY) {
                        ny = desiredY;
                        airborne = false;
                        vy = 0;
                    }
                    onGround = !airborne;
                } else {
                    double blend = Math.min(1.0, step * 2.0);
                    ny = from.getY() + (desiredY - from.getY()) * blend;

                    if (desiredY + 0.3 < from.getY()) {
                        airborne = true;
                        vy = 0;
                        onGround = false;
                    } else onGround = true;
                }
            }

            Location next = new Location(from.getWorld(), nx, ny, nz);

            // Körperausrichtung: nur Yaw aus Bewegungsvektor; Pitch = 0
            float yawBody   = (float) Math.toDegrees(Math.atan2(-mx, mz));
            float pitchBody = 0f;
            float[] body    = new float[]{ yawBody, pitchBody };

            // Head-Mode: sanft + clamp relativ zur Körperausrichtung
            float[] hp = updateHead(true, body[0]);

            next.setYaw(body[0]);
            next.setPitch(body[1]);
            loc = next.clone();

            double dx = loc.getX() - from.getX();
            double dy = loc.getY() - from.getY();
            double dz = loc.getZ() - from.getZ();
            double stepLen = Math.sqrt(dx*dx + dy*dy + dz*dz);

            for (UUID v : viewers.keySet()) {
                Player p = Bukkit.getPlayer(v);
                if (p == null || !p.isOnline()) continue;
                ViewerState vs = viewers.get(v);
                if (vs == null) continue;

                if (stepLen <= MAX_REL_STEP)
                    sendRelMoveLook(p, vs.entityId, dx, dy, dz, body[0], body[1], onGround);
                else
                    sendTeleport(p, loc);

                // Beim Laufen: nur Head-Yaw setzen (Pitch = 0)
                sendHeadRotationOnly(p, vs.entityId, hp[0]);
            }

            long remaining = Math.max(0, waitUntil - now);
            controller.onPathState(asHandle(), idx, dir, remaining);
        }

        private void advanceIndex() {
            if (path.loop == Path.Loop.NONE) {
                if (idx < path.points.size()-1) idx++;
            } else if (path.loop == Path.Loop.LOOP) {
                idx = (idx + 1) % path.points.size();
            } else {
                if (idx == 0) dir = 1;
                if (idx == path.points.size()-1) dir = -1;
                idx += dir;
            }
        }
    }

    private boolean isSolid(org.bukkit.block.Block b) {
        return b != null && b.getType().isSolid();
    }

    private double findSurfaceY(org.bukkit.World w, double x, double startY, double z, int downMax, int upMax) {
        int y = (int) Math.floor(startY);
        for (int i = 0; i < upMax; i++) {
            var bHead = w.getBlockAt((int)Math.floor(x), y + i, (int)Math.floor(z));
            var bFeet = w.getBlockAt((int)Math.floor(x), y + i - 1, (int)Math.floor(z));
            if (!isSolid(bHead) && isSolid(bFeet)) {
                return (y + i - 1) + 1.0;
            }
        }
        for (int i = 0; i < downMax; i++) {
            var bHead = w.getBlockAt((int)Math.floor(x), y - i, (int)Math.floor(z));
            var bFeet = w.getBlockAt((int)Math.floor(x), y - i - 1, (int)Math.floor(z));
            if (!isSolid(bHead) && isSolid(bFeet))
                return (y - i - 1) + 1.0;
        }
        return startY;
    }
}
