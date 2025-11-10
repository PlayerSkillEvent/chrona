package dev.chrona.common.npc;

import com.google.gson.*;
import dev.chrona.common.npc.api.Path;
import dev.chrona.common.npc.api.Waypoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public final class PathUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Logger logger = null;

    private PathUtil() {
        logger = LoggerFactory.getLogger(PathUtil.class);
    }

    /** Lädt einen gespeicherten Pfad (throwt bei Fehler) */
    public static Path loadPath(Plugin plugin, String name) {
        java.nio.file.Path file = pathFile(plugin, name);
        if (!Files.exists(file))
            throw new IllegalArgumentException("Pfad nicht gefunden: " + name);

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            double speed = root.has("speed") ? root.get("speed").getAsDouble() : 3.0;
            var loop = Path.Loop.valueOf(root.has("loop")
                    ? root.get("loop").getAsString().toUpperCase(Locale.ROOT)
                    : "NONE");

            List<Waypoint> points = new ArrayList<>();
            for (var el : root.getAsJsonArray("points")) {
                JsonObject o = el.getAsJsonObject();
                World w = Bukkit.getWorld(o.get("world").getAsString());
                if (w == null)
                    throw new IllegalStateException("Welt nicht gefunden: " + o.get("world").getAsString());

                Location loc = new Location(w,
                        o.get("x").getAsDouble(),
                        o.get("y").getAsDouble(),
                        o.get("z").getAsDouble());
                long wait = o.has("wait") ? o.get("wait").getAsLong() : 0L;
                points.add(new Waypoint(loc, wait));
            }

            if (points.size() < 2)
                throw new IllegalStateException("Pfad benötigt mindestens 2 Punkte.");

            return new Path(name, points, speed, loop);
        } catch (Exception e) {
            logger.error("Fehler beim Laden des Pfades {}", name, e);
            return null;
        }
    }

    /** Speichert (überschreibt) einen Pfad */
    public static void savePath(Plugin plugin,Path path) {
        try {
            Files.createDirectories(pathDir(plugin));
            java.nio.file.Path file = pathFile(plugin, path.name);

            JsonObject root = new JsonObject();
            root.addProperty("speed", path.speedBlocksPerSec);
            root.addProperty("loop", path.loop.toString());
            JsonArray arr = new JsonArray();
            for (var wp : path.points) {
                JsonObject o = new JsonObject();
                o.addProperty("world", wp.loc.getWorld().getName());
                o.addProperty("x", wp.loc.getX());
                o.addProperty("y", wp.loc.getY());
                o.addProperty("z", wp.loc.getZ());
                o.addProperty("wait", wp.waitMs);
                arr.add(o);
            }
            root.add("points", arr);

            try (Writer w = Files.newBufferedWriter(file)) {
                gson.toJson(root, w);
            }
        } catch (Exception e) {
            logger.error("Fehler beim Speichern des Pfades {}", path.name, e);
        }
    }

    public static List<String> listPaths(Plugin plugin) {
        try (var stream = Files.list(pathDir(plugin))) {
            return stream
                    .filter(f -> f.getFileName().toString().endsWith(".json"))
                    .map(f -> f.getFileName().toString().replace(".json", ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static boolean deletePath(Plugin plugin, String name) {
        try {
            return Files.deleteIfExists(pathFile(plugin, name));
        } catch (IOException e) {
            return false;
        }
    }

    // -----------------------------------------------------

    private static java.nio.file.Path pathFile(Plugin plugin, String name) {
        return pathDir(plugin).resolve(name + ".json");
    }

    private static java.nio.file.Path pathDir(Plugin plugin) {
        return plugin.getDataFolder().toPath().resolve("paths");
    }
}
