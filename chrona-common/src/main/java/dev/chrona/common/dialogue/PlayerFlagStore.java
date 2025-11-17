package dev.chrona.common.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.chrona.common.log.ChronaLog;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PlayerFlagStore {
    private final Logger log = ChronaLog.get(PlayerFlagStore.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Map<UUID, Set<String>> flags = new HashMap<>();

    public PlayerFlagStore(Path baseDir) {
        this.file = baseDir.resolve("player_flags.json");
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            var type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> raw = gson.fromJson(reader, type);
            if (raw != null) {
                raw.forEach((k, v) -> flags.put(UUID.fromString(k), new HashSet<>(v)));
            }
            log.info("Loaded flags for {} players from {}", flags.size(), file);
        } catch (Exception e) {
            log.error("Failed to load player flags: {}", e.getMessage(), e);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Map<String, List<String>> raw = new HashMap<>();
            for (var e : flags.entrySet()) {
                raw.put(e.getKey().toString(), new ArrayList<>(e.getValue()));
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                gson.toJson(raw, writer);
            }
        } catch (IOException e) {
            log.error("Failed to save player flags: {}", e.getMessage(), e);
        }
    }

    public synchronized boolean hasFlag(UUID playerId, String key) {
        return flags.getOrDefault(playerId, Set.of()).contains(key);
    }

    public synchronized void setFlag(UUID playerId, String key, boolean value) {
        flags.computeIfAbsent(playerId, k -> new HashSet<>());
        if (value) {
            flags.get(playerId).add(key);
        } else {
            flags.get(playerId).remove(key);
        }
    }
}
