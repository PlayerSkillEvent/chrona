package dev.chrona.plugin.commands;

import com.google.gson.*;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.PathUtil;
import dev.chrona.common.npc.api.Waypoint;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class NpcPathCommand implements CommandExecutor {
    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, RecordingSession> recorders = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDir;

    public NpcPathCommand(Plugin plugin) {
        this.plugin = plugin;
        this.logger = ChronaLog.get(NpcPathCommand.class);
        this.dataDir = plugin.getDataFolder().toPath().resolve("paths");
        try { Files.createDirectories(dataDir); } catch (IOException ignored) {}
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler!");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§7/npcpath start <name>, add [waitMs], end <speed> [loop], list");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> handleStart(p, args);
            case "add" -> handleAdd(p, args);
            case "end" -> handleEnd(p, args);
            case "delete" -> {
                if (args.length < 2) { p.sendMessage("§c/npcpath delete <name>"); return true; }
                boolean ok = PathUtil.deletePath(plugin, args[1]);
                p.sendMessage(ok ? "§aPfad gelöscht." : "§cPfad nicht gefunden.");
            }
            case "list" -> {
                var list = PathUtil.listPaths(plugin);
                if (list.isEmpty()) p.sendMessage("§7Keine Pfade.");
                else p.sendMessage("§aPfade: §f" + String.join(", ", list));
            }
            default -> p.sendMessage("§cUnbekanntes Argument.");
        }
        return true;
    }

    private void handleStart(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cVerwendung: /npcpath start <name>");
            return;
        }
        if (recorders.containsKey(p.getUniqueId())) {
            p.sendMessage("§eDu nimmst bereits einen Pfad auf.");
            return;
        }
        recorders.put(p.getUniqueId(), new RecordingSession(args[1]));
        p.sendMessage("§aAufnahme gestartet für §e" + args[1]);
    }

    private void handleAdd(Player p, String[] args) {
        var rec = recorders.get(p.getUniqueId());
        if (rec == null) {
            p.sendMessage("§cDu hast keine aktive Aufnahme.");
            return;
        }
        long wait = 0;
        if (args.length >= 2) {
            try {
                wait = Long.parseLong(args[1]);
            } catch (NumberFormatException ignored) {}
        }
        rec.points.add(new Waypoint(p.getLocation(), wait));
        p.sendMessage("§aWegpunkt §7" + rec.points.size() + " §ahinzugefügt (§fwait=" + wait + "ms§a)");
    }

    private void handleEnd(Player p, String[] args) {
        var rec = recorders.remove(p.getUniqueId());
        if (rec == null) {
            p.sendMessage("§cDu hast keine aktive Aufnahme.");
            return;
        }
        if (rec.points.size() < 2) {
            p.sendMessage("§cMindestens 2 Wegpunkte benötigt.");
            return;
        }

        double speed = 3.0;
        String loop = "NONE";
        if (args.length >= 2) try { speed = Double.parseDouble(args[1]); } catch (NumberFormatException ignored) {}
        if (args.length >= 3) loop = args[2].toUpperCase(Locale.ROOT);

        savePath(rec.name, rec.points, speed, loop);
        p.sendMessage("§aPfad §e" + rec.name + " §agespeichert (" + rec.points.size() + " Punkte, speed=" + speed + ", loop=" + loop + ")");
    }

    private void savePath(String name, List<Waypoint> points, double speed, String loop) {
        JsonObject root = new JsonObject();
        root.addProperty("speed", speed);
        root.addProperty("loop", loop);
        JsonArray arr = new JsonArray();
        for (var wp : points) {
            JsonObject o = new JsonObject();
            o.addProperty("world", wp.loc.getWorld().getName());
            o.addProperty("x", wp.loc.getX());
            o.addProperty("y", wp.loc.getY());
            o.addProperty("z", wp.loc.getZ());
            o.addProperty("wait", wp.waitMs);
            arr.add(o);
        }
        root.add("points", arr);

        Path file = dataDir.resolve(name + ".json");
        try (Writer w = Files.newBufferedWriter(file)) {
            gson.toJson(root, w);
        } catch (IOException e) {
            logger.warn("Failed to save path {}", name, e);
        }
    }

    private record RecordingSession(String name, List<Waypoint> points) {
        public RecordingSession(String name) {
            this(name, new ArrayList<>());
        }
    }
}
