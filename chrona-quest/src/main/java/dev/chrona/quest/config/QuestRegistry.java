package dev.chrona.quest.config;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.model.QuestDefinition;
import dev.chrona.quest.model.QuestType;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all quest definitions.
 */
public final class QuestRegistry {

    private static final Logger log = ChronaLog.get(QuestRegistry.class);

    private final Map<String, QuestDefinition> byId = new ConcurrentHashMap<>();
    private final Map<QuestType, List<QuestDefinition>> byType = new ConcurrentHashMap<>();

    /** Loads all quest definitions from the given root directory. */
    public void loadFromRootDirectory(File rootDir) {
        byId.clear();
        byType.clear();

        if (!rootDir.exists()) {
            log.warn("Quest root {} does not exist – no quests loaded.", rootDir.getAbsolutePath());
            return;
        }

        List<QuestDefinition> all = new ArrayList<>();
        collectRecursive(rootDir, all);

        for (QuestDefinition def : all) {
            QuestDefinition previous = byId.put(def.id(), def);
            if (previous != null)
                log.warn("Quest id {} twice – later definition overwrites earlier one.", def.id());
            byType.computeIfAbsent(def.type(), t -> new ArrayList<>()).add(def);
        }

        for (var entry : byType.entrySet()) {
            entry.getValue().sort(Comparator
                    .comparingInt(QuestDefinition::sortOrder)
                    .thenComparing(QuestDefinition::id));
        }

        log.info("QuestRegistry laoded: {} quests over {} types.",
                byId.size(), byType.size());
    }

    /** Recursively collects quest definitions from the given directory. */
    private void collectRecursive(File dir, List<QuestDefinition> out) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files) {
            if (f.isDirectory())
                collectRecursive(f, out);
            else if (f.getName().toLowerCase().endsWith(".yml")
                    || f.getName().toLowerCase().endsWith(".yaml")) {
                QuestDefinition def = QuestConfigLoader.loadSingle(f);
                if (def != null)
                    out.add(def);
            }
        }
    }

    /** Gets a quest definition by its ID, if it exists. */
    public Optional<QuestDefinition> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Gets all quest definitions of the given type. */
    public List<QuestDefinition> getByType(QuestType type) {
        return byType.getOrDefault(type, List.of());
    }

    /** Gets all quest definitions. */
    public Collection<QuestDefinition> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }
}
