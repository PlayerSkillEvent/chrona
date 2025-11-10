package dev.chrona.plugin.commands;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.PathUtil;
import dev.chrona.common.npc.api.NpcHandle;
import dev.chrona.common.npc.api.NpcPersistence;
import dev.chrona.common.npc.api.Skin;
import dev.chrona.common.npc.api.SkinService;
import dev.chrona.common.npc.protocol.NpcController;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Locale;

public final class NpcCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final Logger logger;
    private final NpcPersistence.NpcFactory factory;
    private final NpcController ctrl;
    private final SkinService skins;

    public NpcCommand(Plugin plugin, NpcPersistence.NpcFactory factory, NpcController ctrl, SkinService skins) {
        this.plugin = plugin;
        this.logger = ChronaLog.get(NpcCommand.class);
        this.factory = factory;
        this.ctrl = ctrl;
        this.skins = skins;
    }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler.");
            return true;
        }

        if (args.length == 0) {
            help(player);
            return true;
        }

        if (!player.hasPermission("chrona.npc")) {
            player.sendMessage("§cKeine Rechte.");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "create" -> {
                    if (args.length < 2) {
                        player.sendMessage("§c/npc create <name> [skinPlayer]");
                        break;
                    }

                    String name = args[1];
                    if (ctrl.exists(name)) {
                        player.sendMessage("§cNPC existiert schon.");
                        break;
                    }

                    Skin skin = null;
                    if (args.length >= 3) {
                        String skinName = args[2];
                        player.sendMessage("§7Skin laden von Mojang…");
                        skin = skins.fetchByPlayerName(skinName);
                    }
                    Location loc = player.getLocation();
                    NpcHandle npc = factory.create(loc, name, skin);
                    ctrl.register(name, npc);
                    npc.addViewer(player);
                    player.sendMessage("§aNPC §e" + name + " §abei dir erstellt.");
                }
                case "delete" -> {
                    if (args.length < 2) {
                        player.sendMessage("§c/npc delete <name>");
                        break;
                    }
                    ctrl.unregister(args[1]);
                    player.sendMessage("§aNPC gelöscht: §e" + args[1]);
                }
                case "list" -> player.sendMessage("§aNPCs: §f" + String.join(", ", ctrl.listNames()));
                case "name" -> {
                    if (args.length < 3) {
                        player.sendMessage("§c/npc name <name> <newName>");
                        break;
                    }
                    var npc = ctrl.get(args[1]);
                    if (npc == null) {
                        player.sendMessage("§cUnbekannt.");
                        break;
                    }
                    npc.setName(args[2]);
                    player.sendMessage("§aUmbenannt.");
                }
                case "skin" -> {
                    if (args.length < 3) {
                        player.sendMessage("§c/npc skin <name> <playerSkinName>");
                        break;
                    }
                    var npc = ctrl.get(args[1]);
                    if (npc == null) {
                        player.sendMessage("§cUnbekannt.");
                        break;
                    }
                    player.sendMessage("§7Skin laden…");
                    Skin sNew = skins.fetchByPlayerName(args[2]);
                    npc.setSkin(sNew);
                    player.sendMessage("§aSkin gesetzt auf §e" + args[2]);
                }
                case "look" -> {
                    if (args.length < 3) {
                        player.sendMessage("§c/npc look <name> FIXED|TRACK [radius]");
                        break;
                    }

                    var npc = ctrl.get(args[1]);
                    if (npc == null) {
                        player.sendMessage("§cUnbekannt.");
                        break;
                    }
                    String m = args[2].toUpperCase(Locale.ROOT);
                    double radius = args.length >= 4 ? Double.parseDouble(args[3]) : 12.0;
                    NpcController.LookMode mode = switch (m) {
                        case "TRACK", "FOLLOW" -> NpcController.LookMode.TRACK_PLAYER;
                        default -> NpcController.LookMode.FIXED;
                    };
                    if (mode == NpcController.LookMode.FIXED)
                        npc.setRotationSource(null);
                    else npc.setRotationSource(() -> {
                        var o = npc.location();
                        double r2 = radius*radius;
                        Player best = null;
                        double bestD = Double.MAX_VALUE;
                        for (Player pl : o.getWorld().getPlayers()) {
                            double d2 = pl.getLocation().distanceSquared(o);
                            if (d2 <= r2 && d2 < bestD) {
                                best=pl;
                                bestD=d2;
                            }
                        }
                        return best != null ? best.getEyeLocation() : null;
                    });
                    ctrl.setLookMode(npc, mode, radius);
                    player.sendMessage("§aLook-Mode: §e" + mode + " §7(r=" + radius + ")");
                }
                case "tp" -> {
                    if (args.length < 3) {
                        player.sendMessage("§c/npc tp <name> here | <x> <y> <z> [world]");
                        break;
                    }

                    var npc = ctrl.get(args[1]);
                    if (npc == null) {
                        player.sendMessage("§cUnbekannt.");
                        break;
                    }
                    Location to;
                    if (args[2].equalsIgnoreCase("here"))
                        to = player.getLocation();
                    else {
                        double x = Double.parseDouble(args[2]);
                        double y = Double.parseDouble(args[3]);
                        double z = Double.parseDouble(args[4]);
                        World w = args.length >= 6 ? Bukkit.getWorld(args[5]) : player.getWorld();
                        to = new Location(w, x, y, z);
                    }
                    npc.teleport(to);
                    player.sendMessage("§aTeleportiert.");
                }
                case "runpath" -> {
                    if (args.length < 3) { player.sendMessage("§c/npc runpath <name> <pathName>"); break; }
                    var npc = ctrl.get(args[1]);
                    if (npc == null) {
                        player.sendMessage("§cUnbekannt.");
                        break;
                    }
                    var path = PathUtil.loadPath(plugin, args[2]);
                    npc.runPath(path);
                    player.sendMessage("§aPfad gestartet.");
                }
                case "stoppath" -> {
                    if (args.length < 2) {
                        player.sendMessage("§c/npc stoppath <name>");
                        break;
                    }
                    var npc = ctrl.get(args[1]);
                    if (npc == null) {
                        player.sendMessage("§cUnbekannt.");
                        break;
                    }
                    npc.stopPath();
                    player.sendMessage("§aPfad gestoppt.");
                }
                default -> help(player);
            }
        }
        catch (Exception ex) {
            player.sendMessage("§cFehler: " + ex.getMessage());
            logger.error("Error in NPC-Command", ex);
        }
        return true;
    }

    private void help(Player p) {
        p.sendMessage("""
                §e/npc create <name> [skinPlayer]
                §e/npc delete <name>
                §e/npc list
                §e/npc name <name> <newName>
                §e/npc skin <name> <playerSkinName>
                §e/npc look <name> FIXED|TRACK [radius]
                §e/npc tp <name> here|<x y z [world]>
                §e/npc runpath <name> <pathName>
                §e/npc stoppath <name>
                """);
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, String @NotNull [] a) {
        return java.util.Collections.emptyList();
    }
}
