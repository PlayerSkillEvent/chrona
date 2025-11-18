package dev.chrona.common.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.chrona.common.log.ChronaLog;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public final class DialogueRegistry {
    private final Logger log = ChronaLog.get(DialogueRegistry.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dir;
    private final Map<String, Dialogue> byId = new HashMap<>();

    public DialogueRegistry(Path baseDir) {
        this.dir = baseDir.resolve("dialogues");
        reload();
    }

    /** Reloads all dialogues from disk. */
    public synchronized void reload() {
        byId.clear();
        try {
            Files.createDirectories(dir);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
                for (Path p : ds) {
                    try (BufferedReader reader = Files.newBufferedReader(p)) {
                        Dialogue d = gson.fromJson(reader, Dialogue.class);
                        if (d == null || d.getId() == null) {
                            log.warn("Skipping dialogue file {} (no id)", p.getFileName());
                            continue;
                        }
                        byId.put(d.getId(), d);
                    }
                    catch (Exception e) {
                        log.error("Failed to load dialogue {}: {}", p.getFileName(), e.getMessage(), e);
                    }
                }
            }
            log.info("Loaded {} dialogues from {}", byId.size(), dir);
        }
        catch (IOException e) {
            log.error("Failed to load dialogues from {}: {}", dir, e.getMessage(), e);
        }
    }

    public Dialogue get(String id) {
        return byId.get(id);
    }
}
