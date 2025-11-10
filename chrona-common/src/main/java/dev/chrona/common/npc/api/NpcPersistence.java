package dev.chrona.common.npc.api;

import com.google.gson.*;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.PathUtil;
import dev.chrona.common.npc.protocol.NpcController;
import org.bukkit.*;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class NpcPersistence {
    private final Plugin plugin;
    private final Logger logger;
    private final NpcController ctrl;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final java.nio.file.Path file;

    public interface NpcFactory {
        NpcHandle create(Location loc, String name, Skin skin);
    }

    private final NpcFactory factory;

    public NpcPersistence(Plugin plugin, NpcController ctrl, NpcFactory factory) {
        this.plugin = plugin;
        this.logger = ChronaLog.get(NpcPersistence.class);
        this.ctrl = ctrl;
        this.factory = factory;
        this.file = plugin.getDataFolder().toPath().resolve("npcs.json");
        try { Files.createDirectories(plugin.getDataFolder().toPath()); } catch (IOException ignored) {}
    }

    // -------- SAVE --------

    public synchronized void saveAll(Collection<NpcRuntime> runtimes) {
        JsonArray arr = new JsonArray();
        for (var rt : runtimes)
            arr.add(toJson(rt));
        try (Writer w = Files.newBufferedWriter(file)) {
            gson.toJson(arr, w);
        }
        catch (IOException e) {
            logger.warn("Failed to save NPCs", e);
        }
        logger.info("Saved {} NPCs.", runtimes.size());
    }

    private JsonObject toJson(NpcRuntime rt) {
        var s = new JsonObject();
        var loc = rt.npc.location();
        s.addProperty("id", rt.internalId.toString());
        s.addProperty("name", rt.npc.name());
        s.addProperty("skinValue", rt.skin != null ? rt.skin.value() : null);
        s.addProperty("skinSignature", rt.skin != null ? rt.skin.signature() : null);
        s.addProperty("world", loc.getWorld().getName());
        s.addProperty("x", loc.getX());
        s.addProperty("y", loc.getY());
        s.addProperty("z", loc.getZ());
        s.addProperty("yaw", loc.getYaw());
        s.addProperty("pitch", loc.getPitch());
        s.addProperty("lookMode", rt.lookMode.name());
        s.addProperty("lookRadius", rt.lookRadius);

        // Path
        s.addProperty("pathRunning", rt.pathRunning);
        s.addProperty("pathName", rt.pathName);
        s.addProperty("pathIndex", rt.pathIndex);
        s.addProperty("pathDir", rt.pathDir);
        s.addProperty("waitRemainingMs", Math.max(0, rt.waitRemainingMs));
        s.addProperty("speed", rt.pathSpeed);
        s.addProperty("loop", rt.pathLoop != null ? rt.pathLoop.name() : null);
        return s;
    }

    // -------- LOAD --------

    public synchronized List<NpcRuntime> loadAllAndRecreate() {
        if (!Files.exists(file))
            return List.of();

        List<NpcRuntime> result = new ArrayList<>();
        try (Reader r = Files.newBufferedReader(file)) {
            var arr = JsonParser.parseReader(r).getAsJsonArray();
            for (var el : arr) {
                var o = el.getAsJsonObject();
                String worldName = o.get("world").getAsString();
                World w = Bukkit.getWorld(worldName);
                if (w == null) {
                    logger.warn("NPC skipped: world not found {}", worldName);
                    continue;
                }
                Location loc = new Location(w,
                        o.get("x").getAsDouble(),
                        o.get("y").getAsDouble(),
                        o.get("z").getAsDouble(),
                        o.get("yaw").getAsFloat(),
                        o.get("pitch").getAsFloat());

                String name = o.get("name").getAsString();
                String skinValue = optStr(o, "skinValue");
                String skinSig = optStr(o, "skinSignature");
                Skin skin = (skinValue != null && skinSig != null) ? new Skin(skinValue, skinSig) : null;

                NpcHandle npc = factory.create(loc, name, skin);
                ctrl.register(name, npc);
                npc.setSkin(skin);

                var rt = new NpcRuntime(npc);
                rt.internalId = UUID.fromString(o.get("id").getAsString());
                rt.lookMode = NpcController.LookMode.valueOf(o.get("lookMode").getAsString());
                rt.lookRadius = o.get("lookRadius").getAsDouble();
                ctrlApplyLookMode(rt);

                rt.pathRunning = o.get("pathRunning").getAsBoolean();
                rt.pathName = optStr(o, "pathName");
                rt.pathIndex = o.get("pathIndex").getAsInt();
                rt.pathDir = o.get("pathDir").getAsInt();
                rt.waitRemainingMs = o.get("waitRemainingMs").getAsLong();
                rt.pathSpeed = o.get("speed").getAsDouble();
                rt.pathLoop = optStr(o, "loop") != null ? Path.Loop.valueOf(o.get("loop").getAsString()) : null;

                if (rt.pathRunning && rt.pathName != null) {
                    try {
                        Path p = PathUtil.loadPath(plugin, rt.pathName);
                        if (rt.pathSpeed > 0)
                            p = override(p, rt.pathSpeed, rt.pathLoop != null ? rt.pathLoop : p.loop);
                        npc.runPath(resumeFromIndex(p, rt));
                    } catch (Exception ex) {
                        logger.warn("Failed to resume path for NPC {}", name, ex);
                    }
                }

                result.add(rt);
            }
        } catch (Exception e) {
            logger.warn("Failed to load NPCs", e);
        }
        return result;
    }

    private static String optStr(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private Path override(Path base, double speed, Path.Loop loop) {
        return Path.builder()
                .speed(speed)
                .loop(loop)
                .addAll(base.points) // -> bau dir in Path.Builder eine addAll(List<Waypoint>) ein
                .build();
    }

    private Path resumeFromIndex(Path p, NpcRuntime rt) {
        return p;
    }

    private void ctrlApplyLookMode(NpcRuntime rt) {
        if (rt.lookMode == NpcController.LookMode.FIXED)
            rt.npc.setRotationSource(null);
        else {
            rt.npc.setRotationSource(() -> {
                var o = rt.npc.location();
                double r2 = rt.lookRadius * rt.lookRadius;
                Player best = null; double bestD = Double.MAX_VALUE;
                for (Player pl : o.getWorld().getPlayers()) {
                    double d2 = pl.getLocation().distanceSquared(o);
                    if (d2 <= r2 && d2 < bestD) { best = pl; bestD = d2; }
                }
                return best != null ? best.getEyeLocation() : null;
            });
        }
    }

    public static final class NpcRuntime {
        public final NpcHandle npc;
        public UUID internalId;
        public Skin skin;

        public NpcController.LookMode lookMode = NpcController.LookMode.FIXED;
        public double lookRadius = 12.0;

        public boolean pathRunning = false;
        public String pathName = null;
        public int pathIndex = 0;
        public int pathDir = 1;
        public long waitRemainingMs = 0;
        public double pathSpeed = 3.0;
        public Path.Loop pathLoop = Path.Loop.NONE;

        public NpcRuntime(NpcHandle npc) { this.npc = npc; }
    }
}
