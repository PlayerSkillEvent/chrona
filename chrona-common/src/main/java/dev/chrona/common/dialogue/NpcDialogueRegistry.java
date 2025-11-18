package dev.chrona.common.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.chrona.common.log.ChronaLog;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Registry for NPC dialogue bindings loaded from npc_bindings.json. */
public final class NpcDialogueRegistry {
    private final Logger log = ChronaLog.get(NpcDialogueRegistry.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Map<String, List<NpcDialogueBinding>> byNpc = new HashMap<>();

    public NpcDialogueRegistry(Plugin plugin, Path baseDir) {
        this.file = baseDir.resolve("npc_bindings.json");
        reload();
    }

    /** Reloads all NPC dialogue bindings from disk. */
    public synchronized void reload() {
        byNpc.clear();
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "[]");
            }
            catch (IOException e) {
                log.error("Failed to create npc_bindings.json: {}", e.getMessage(), e);
            }
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            var listType = new TypeToken<List<NpcDialogueBinding>>(){}.getType();
            List<NpcDialogueBinding> bindings = gson.fromJson(reader, listType);

            if (bindings == null)
                return;
            for (NpcDialogueBinding b : bindings)
                byNpc.computeIfAbsent(b.getNpcName(), k -> new ArrayList<>()).add(b);

            for (List<NpcDialogueBinding> list : byNpc.values())
                list.sort(Comparator.comparing(NpcDialogueBinding::getDialogueId));

            log.info("Loaded {} NPC dialogue bindings from {}", bindings.size(), file);
        }
        catch (Exception e) {
            log.error("Failed to load NPC dialogue bindings: {}", e.getMessage(), e);
        }
    }

    /** Gets all dialogue bindings for the given NPC name. */
    public List<NpcDialogueBinding> getBindings(String npcName) {
        return byNpc.getOrDefault(npcName, List.of());
    }
}
