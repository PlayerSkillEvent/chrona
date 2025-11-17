package dev.chrona.common.dialogue;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DialogueTextUtil {
    private DialogueTextUtil() {}

    public static List<String> resolveText(Map<String, List<String>> text, Player player) {
        if (text == null || text.isEmpty()) return null;
        String langKey = resolveLangKey(player);
        if (text.containsKey(langKey)) return text.get(langKey);
        if (text.containsKey("en")) return text.get("en");
        return text.values().stream().findFirst().orElse(null);
    }

    public static String resolveChoiceText(Map<String, String> text, Player player) {
        if (text == null || text.isEmpty()) return null;
        String langKey = resolveLangKey(player);
        if (text.containsKey(langKey)) return text.get(langKey);
        if (text.containsKey("en")) return text.get("en");
        return text.values().stream().findFirst().orElse(null);
    }

    private static String resolveLangKey(Player player) {
        try {
            String locale = player.locale().toString(); // e.g. en_us
            String[] parts = locale.split("_");
            return parts[0].toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return "en";
        }
    }
}
