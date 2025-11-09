package dev.chrona.common.npc.protocol;

import dev.chrona.common.npc.api.NpcHandle;
import dev.chrona.common.npc.api.NpcPersistence;
import dev.chrona.common.npc.api.Path;
import dev.chrona.common.npc.api.Skin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcController {
    private final Map<String, NpcHandle> byName = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lookTaskByNpc = new ConcurrentHashMap<>();
    private final Map<UUID, LookMode> modeByNpc = new ConcurrentHashMap<>();
    private final Map<UUID, Double> radiusByNpc = new ConcurrentHashMap<>();
    private final Map<String, NpcPersistence.NpcRuntime> runtimeByName = new ConcurrentHashMap<>();

    public Collection<String> listNames() { return List.copyOf(byName.keySet()); }
    public NpcHandle get(String name) { return byName.get(name.toLowerCase()); }
    public boolean exists(String name){ return byName.containsKey(name.toLowerCase()); }

    public void register(String name, NpcHandle npc) {
        byName.put(name.toLowerCase(), npc);
        var rt = new NpcPersistence.NpcRuntime(npc);
        rt.internalId = npc.id();
        rt.skin = null;
        rt.lookMode = LookMode.FIXED;
        rt.lookRadius = 12.0;
        runtimeByName.put(name.toLowerCase(), rt);
    }

    public void unregister(String name) {
        NpcHandle npc = byName.remove(name.toLowerCase());
        runtimeByName.remove(name.toLowerCase());
        if (npc != null) {
            stopAutoLook(npc);
            npc.destroy();
        }
    }

    public Collection<NpcPersistence.NpcRuntime> runtimes() {
        return List.copyOf(runtimeByName.values());
    }

    public void setLookMode(NpcHandle npc, LookMode mode, double radius) {
        if (mode == LookMode.FIXED) {
            npc.setRotationSource(null);
            return;
        }
        npc.setRotationSource(() -> {
            var origin = npc.location();
            var world = origin.getWorld();
            double r2 = radius * radius;
            Player best = null; double bestD = Double.MAX_VALUE;
            for (Player p : world.getPlayers()) {
                double d2 = p.getLocation().distanceSquared(origin);
                if (d2 <= r2 && d2 < bestD) { best = p; bestD = d2; }
            }
            return best != null ? best.getEyeLocation() : null;
        });
        runtimeByName.get(npc.name().toLowerCase()).lookMode = mode;
        runtimeByName.get(npc.name().toLowerCase()).lookRadius = radius;
    }

    public void stopAutoLook(NpcHandle npc) {
        Integer t = lookTaskByNpc.remove(npc.id());
        if (t != null) Bukkit.getScheduler().cancelTask(t);
    }

    public void onPathStarted(NpcHandle npc, String pathName, Path p) {
        var rt = runtimeByName.get(npc.name().toLowerCase());
        rt.pathRunning = true; rt.pathName = pathName;
        rt.pathSpeed = p.speedBlocksPerSec; rt.pathLoop = p.loop;
        rt.pathIndex = 0; rt.pathDir = 1; rt.waitRemainingMs = 0;
    }
    public void onPathState(NpcHandle npc, int index, int dir, long waitRemaining) {
        var rt = runtimeByName.get(npc.name().toLowerCase());
        rt.pathIndex = index; rt.pathDir = dir; rt.waitRemainingMs = waitRemaining;
    }
    public void onPathStopped(NpcHandle npc) {
        var rt = runtimeByName.get(npc.name().toLowerCase());
        rt.pathRunning = false; rt.waitRemainingMs = 0;
    }
    public void onSkinSet(NpcHandle npc, Skin s) {
        runtimeByName.get(npc.name().toLowerCase()).skin = s;
    }

    public enum LookMode { FIXED, TRACK_PLAYER }
}
