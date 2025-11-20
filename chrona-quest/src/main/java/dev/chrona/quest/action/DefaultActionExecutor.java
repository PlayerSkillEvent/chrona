package dev.chrona.quest.action;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.model.ActionDef;
import dev.chrona.quest.model.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of an action executor that processes various action types
 * defined in quests and performs the corresponding operations within the game context.
 */
public final class DefaultActionExecutor {

    private static final Logger log = ChronaLog.get(DefaultActionExecutor.class);

    /** Executes a list of actions within the given execution context.
     *
     * @param actions the list of actions to execute
     * @param ctx     the action execution context
     */
    public void executeAll(List<ActionDef> actions, ActionExecutionContext ctx) {
        if (actions == null || actions.isEmpty() || ctx == null)
            return;
        for (ActionDef action : actions)
            execute(action, ctx);
    }

    /** Executes a single action within the given execution context.
     *
     * @param action the action to execute
     * @param ctx    the action execution context
     */
    public void execute(ActionDef action, ActionExecutionContext ctx) {
        if (action == null || ctx == null)
            return;

        ActionType type = action.type();
        Map<String, Object> params = action.params();
        UUID playerId = ctx.playerId();
        Player player = ctx.player();

        switch (type) {
            case SET_FLAG -> {
                String key = asString(params.get("key"));
                Boolean value = asBool(params.get("value"), true);
                if (key == null) {
                    log.warn("SET_FLAG without key for player {}", playerId);
                    return;
                }
                ctx.setFlag(playerId, key, value);
            }

            case CLEAR_FLAG -> {
                String key = asString(params.get("key"));
                if (key == null) {
                    log.warn("CLEAR_FLAG without key for player {}", playerId);
                    return;
                }
                ctx.setFlag(playerId, key, false);
            }

            case DIALOGUE_START -> {
                if (player == null)
                    return;
                String npcId = asString(params.get("npcId"));
                String dialogueId = asString(params.get("dialogueId"));
                if (npcId == null || dialogueId == null) {
                    log.warn("DIALOGUE_START without npcId/dialogueId for player {}", playerId);
                    return;
                }
                ctx.startDialogue(player, npcId, dialogueId);
            }

            case TELEPORT -> {
                if (player == null)
                    return;
                String worldName = asString(params.get("world"));
                Integer x = asInt(params.get("x"));
                Integer y = asInt(params.get("y"));
                Integer z = asInt(params.get("z"));
                if (worldName == null || x == null || y == null || z == null) {
                    log.warn("TELEPORT without complete coordinates/world for player {}", playerId);
                    return;
                }
                var world = Bukkit.getWorld(worldName);
                if (world == null) {
                    log.warn("TELEPORT: unknown world {} for player {}", worldName, playerId);
                    return;
                }
                Location loc = new Location(world, x + 0.5, y, z + 0.5);
                ctx.teleport(player, loc);
            }

            case MESSAGE -> {
                if (player == null)
                    return;
                String text = asString(params.get("text"));

                if (text == null)
                    return;
                ctx.sendMessage(player, color(text));
            }

            case TITLE -> {
                if (player == null)
                    return;

                String title = color(asString(params.getOrDefault("title", "")));
                String subtitle = color(asString(params.getOrDefault("subtitle", "")));
                int fadeIn = asInt(params.getOrDefault("fadeIn", 10));
                int stay = asInt(params.getOrDefault("stay", 40));
                int fadeOut = asInt(params.getOrDefault("fadeOut", 10));
                ctx.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }

            case SOUND -> {
                if (player == null)
                    return;
                String soundKey = asString(params.get("sound"));
                float volume = (float) asDouble(params.getOrDefault("volume", 1.0));
                float pitch = (float) asDouble(params.getOrDefault("pitch", 1.0));

                if (soundKey == null) {
                    log.warn("SOUND without sound-Key for player {}", playerId);
                    return;
                }
                ctx.playSound(player, soundKey, volume, pitch);
            }

            case RUN_COMMAND -> {
                String command = asString(params.get("command"));
                if (command == null || command.isBlank()) {
                    log.warn("RUN_COMMAND without command for player {}", playerId);
                    return;
                }
                ctx.runConsoleCommand(command);
            }

            case GIVE_ITEM -> {
                if (player == null)
                    return;
                String itemId = asString(params.get("itemId"));
                Integer amount = asInt(params.getOrDefault("amount", 1));
                Boolean bind = asBool(params.get("bindOnPickup"), false);
                if (itemId == null) {
                    log.warn("GIVE_ITEM without itemId for player {}", playerId);
                    return;
                }
                ctx.giveItem(player, itemId, amount != null ? amount : 1, bind);
            }

            case WORLD_STATE_SET -> {
                String key = asString(params.get("key"));
                Integer value = asInt(params.get("value"));
                Integer incrementBy = asInt(params.get("incrementBy"));
                if (key == null) {
                    log.warn("WORLD_STATE_SET without key for player {}", playerId);
                    return;
                }
                ctx.setWorldState(key, value, incrementBy);
            }

            case BROADCAST -> {
                String msg = asString(params.get("message"));
                if (msg == null)
                    return;

                ctx.broadcast(color(msg));
            }

            default -> log.warn("Unknown ActionType {} for player {}", type, playerId);
        }
    }

    /** Converts an object to a String.
     *
     * @param o the object to convert
     * @return the string representation, or null if the object is null
     */
    private static String asString(Object o) {
        return o != null ? String.valueOf(o) : null;
    }

    /** Converts an object to an Integer.
     *
     * @param o the object to convert
     * @return the integer value, or null if conversion is not possible
     */
    private static Integer asInt(Object o) {
        if (o instanceof Number n)
            return n.intValue();
        if (o == null)
            return null;
        try {
            return Integer.parseInt(String.valueOf(o));
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /** Converts an object to a double.
     *
     * @param o the object to convert
     * @return the double value, or 0.0 if conversion is not possible
     */
    private static double asDouble(Object o) {
        if (o instanceof Number n)
            return n.doubleValue();
        if (o == null)
            return 0.0;
        try {
            return Double.parseDouble(String.valueOf(o));
        }
        catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Converts an object to a Boolean.
     *
     * @param o          the object to convert
     * @param defaultVal the default value if the object is null
     * @return the boolean value
     */
    private static Boolean asBool(Object o, boolean defaultVal) {
        if (o instanceof Boolean b)
            return b;
        if (o == null)
            return defaultVal;

        return Boolean.parseBoolean(String.valueOf(o));
    }

    /** Colors a message string using Minecraft color codes.
     *
     * @param msg the message to color
     * @return the colored message
     */
    private static String color(String msg) {
        if (msg == null)
            return null;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
