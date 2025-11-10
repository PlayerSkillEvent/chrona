package dev.chrona.plugin.commands;

import com.google.gson.*;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.PathUtil;
import dev.chrona.common.npc.api.Path.Loop;
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
    private final Map<UUID, RecordingSession> recorders = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public NpcPathCommand(Plugin plugin) {
        this.plugin = plugin;
        Path dataDir = plugin.getDataFolder().toPath().resolve("paths");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException ignored) {}
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

        switch (args[0].toLowerCase()) {
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
        Loop loop = Loop.NONE;
        if (args.length >= 2)
            try {
                speed = Double.parseDouble(args[1]);
            } catch (NumberFormatException ignored) {}
        if (args.length >= 3)
            loop = Loop.valueOf(args[2]);

        PathUtil.savePath(plugin, new dev.chrona.common.npc.api.Path(rec.name, rec.points, speed, loop));
        p.sendMessage("§aPfad §e" + rec.name + " §agespeichert (" + rec.points.size() + " Punkte, speed=" + speed + ", loop=" + loop + ")");
    }

    private record RecordingSession(String name, List<Waypoint> points) {
        public RecordingSession(String name) {
            this(name, new ArrayList<>());
        }
    }
}
