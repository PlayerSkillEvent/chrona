package dev.chrona.quest.config;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;

/** Loader for quest definitions from YAML files. */
public final class QuestConfigLoader {

    private static final Logger log = ChronaLog.get(QuestConfigLoader.class);

    private QuestConfigLoader() {}

    /** Loads all quest definitions from YAML files in the given directory. */
    public static List<QuestDefinition> loadFromDirectory(File questsDir) {
        List<QuestDefinition> result = new ArrayList<>();

        if (!questsDir.exists()) {
            log.warn("Quest directory {} does not exist – no quests loaded.", questsDir.getAbsolutePath());
            return result;
        }

        File[] files = questsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".yml") || name.toLowerCase().endsWith(".yaml"));
        if (files == null)
            return result;

        for (File file : files) {
            try {
                QuestDefinition def = loadSingle(file);
                if (def != null)
                    result.add(def);
            }
            catch (Exception e) {
                log.error("Error loading from quest file {}: {}", file.getName(), e.getMessage(), e);
            }
        }

        log.info("QuestConfigLoader: {} quests loaded from {}.", result.size(), questsDir.getAbsolutePath());
        return result;
    }

    /** Loads a single quest definition from the given YAML file. */
    public static QuestDefinition loadSingle(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String id = cfg.getString("id");
        if (id == null || id.isBlank()) {
            log.warn("Quest in {} has invalid id – ignored.", file.getName());
            return null;
        }

        int version = cfg.getInt("version", 1);

        QuestType type;
        String typeStr = cfg.getString("type", "SIDE");
        try {
            type = QuestType.valueOf(typeStr.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            log.warn("Quest {} has invali type {} – ignoredd.", id, typeStr);
            return null;
        }

        QuestRepeatability repeatability;
        String repStr = cfg.getString("repeatability", "ONCE");
        try {
            repeatability = QuestRepeatability.valueOf(repStr.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            log.warn("Quest {} has invalid repeatability {} – ignored.", id, repStr);
            return null;
        }

        String arc = cfg.getString("arc", null);
        Integer chapter = cfg.contains("chapter") ? cfg.getInt("chapter") : null;
        String category = cfg.getString("category", null);

        String title = cfg.getString("title", id);
        String shortTitle = cfg.getString("shortTitle", title);
        String description = cfg.getString("description", "");

        ConfigurationSection uiSec = cfg.getConfigurationSection("ui");
        boolean showInLog = uiSec != null && uiSec.getBoolean("showInLog", true);
        int sortOrder = uiSec != null ? uiSec.getInt("sortOrder", 0) : 0;
        String icon = uiSec != null ? uiSec.getString("icon", "PAPER") : "PAPER";

        ConfigurationSection flowSec = cfg.getConfigurationSection("flow");
        QuestFlowMode flowMode = QuestFlowMode.SEQUENTIAL;
        boolean autoStart = true;
        boolean autoCompleteOnLast = true;

        if (flowSec != null) {
            String modeStr = flowSec.getString("mode", "SEQUENTIAL");
            try {
                flowMode = QuestFlowMode.valueOf(modeStr.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                log.warn("Quest {} has invalid flow.mode {} – use SEQUENTIAL.", id, modeStr);
            }
            autoStart = flowSec.getBoolean("autoStartOnAccept", true);
            autoCompleteOnLast = flowSec.getBoolean("autoCompleteOnLastObjective", true);
        }

        // Requirements
        QuestRequirements requirements = new QuestRequirements(
                parseConditionLogic(cfg.getConfigurationSection("requirements"))
        );

        // Objectives
        List<ObjectiveDef> objectives = parseObjectives(cfg.getMapList("objectives"), id);

        // Rewards
        RewardDef rewards = parseRewards(cfg.getConfigurationSection("rewards"));

        // Timing
        QuestTiming timing = parseTiming(cfg.getConfigurationSection("timing"));

        return new QuestDefinition(
                id,
                version,
                type,
                repeatability,
                arc,
                chapter,
                category,
                title,
                shortTitle,
                description,
                showInLog,
                sortOrder,
                icon,
                flowMode,
                autoStart,
                autoCompleteOnLast,
                requirements,
                objectives,
                rewards,
                timing
        );
    }

    /** Parses condition logic from the given configuration section. */
    private static ConditionLogic parseConditionLogic(ConfigurationSection sec) {
        if (sec == null)
            return new ConditionLogic(null, null, null);

        List<ConditionDef> allOf = parseConditions(sec.getMapList("allOf"));
        List<ConditionDef> anyOf = parseConditions(sec.getMapList("anyOf"));
        List<ConditionDef> noneOf = parseConditions(sec.getMapList("noneOf"));
        return new ConditionLogic(allOf, anyOf, noneOf);
    }

    /** Parses a list of condition definitions from a list of maps. */
    private static List<ConditionDef> parseConditions(List<Map<?, ?>> list) {
        if (list == null)
            return List.of();
        List<ConditionDef> out = new ArrayList<>();

        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> raw))
                continue;

            Map<String, Object> map = new HashMap<>();
            raw.forEach((k, v) -> map.put(String.valueOf(k), v));

            String typeStr = (String) map.remove("type");
            if (typeStr == null)
                continue;
            ConditionType type;
            try {
                type = ConditionType.valueOf(typeStr.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                log.warn("Unknown ConditionType {} – ignored.", typeStr);
                continue;
            }
            out.add(new ConditionDef(type, map));
        }
        return out;
    }

    private static List<ObjectiveDef> parseObjectives(List<Map<?, ?>> list, String questId) {
        if (list == null)
            return List.of();
        List<ObjectiveDef> result = new ArrayList<>();

        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> raw))
                continue;
            Map<String, Object> map = new HashMap<>();
            raw.forEach((k, v) -> map.put(String.valueOf(k), v));

            String id = (String) map.remove("id");
            String typeStr = (String) map.remove("type");
            if (id == null || typeStr == null) {
                log.warn("Objective without id or type in quest {} – ignored.", questId);
                continue;
            }

            ObjectiveType type;
            try {
                type = ObjectiveType.valueOf(typeStr.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                log.warn("Objective {} in quest {} has unknown type {} – ignored.",
                        id, questId, typeStr);
                continue;
            }

            String title = (String) map.remove("title");
            String description = (String) map.remove("description");
            String trackerText = (String) map.remove("trackerText");

            long target = 1;
            Object targetObj = map.remove("target");
            if (targetObj instanceof Number n)
                target = n.longValue();
            else if (targetObj != null) {
                try {
                    target = Long.parseLong(String.valueOf(targetObj));
                } catch (NumberFormatException ignored) {}
            }

            String countMode = (String) map.remove("countMode");

            // autoComplete
            Object autoCompleteObj = map.getOrDefault("autoComplete", true);
            boolean autoComplete = Boolean.parseBoolean(String.valueOf(autoCompleteObj));
            map.remove("autoComplete");

            // allowPartialProgress
            Object allowPartialObj = map.getOrDefault("allowPartialProgress", true);
            boolean allowPartial = Boolean.parseBoolean(String.valueOf(allowPartialObj));
            map.remove("allowPartialProgress");

            // requirements:
            ConditionLogic reqLogic = null;
            Object reqObj = map.remove("requirements");
            if (reqObj instanceof Map<?, ?> reqMap) {
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.createSection("requirements", reqMap);
                reqLogic = parseConditionLogic(tmp.getConfigurationSection("requirements"));
            }

            // onStart / onComplete:
            List<ActionDef> onStart = List.of();
            List<ActionDef> onComplete = List.of();

            Object onStartObj = map.remove("onStart");
            if (onStartObj instanceof List<?> rawList)
                onStart = parseActions(rawList);

            Object onCompleteObj = map.remove("onComplete");
            if (onCompleteObj instanceof List<?> rawList)
                onComplete = parseActions(rawList);


            // Map now contains type-specific fields (npcId, regionId, itemId, etc.)
            ObjectiveDef def = new ObjectiveDef(
                    id,
                    type,
                    title,
                    description,
                    trackerText,
                    target,
                    countMode,
                    autoComplete,
                    allowPartial,
                    reqLogic,
                    onStart,
                    onComplete,
                    map
            );
            result.add(def);
        }

        return result;
    }

    /** Parses a list of action definitions from a list of objects. */
    private static List<ActionDef> parseActions(List<?> list) {
        List<ActionDef> out = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> raw))
                continue;
            Map<String, Object> map = new HashMap<>();
            raw.forEach((k, v) -> map.put(String.valueOf(k), v));

            String typeStr = (String) map.remove("type");
            if (typeStr == null)
                continue;

            ActionType type;
            try {
                type = ActionType.valueOf(typeStr.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                log.warn("Unknown ActionType {} – ignored.", typeStr);
                continue;
            }

            out.add(new ActionDef(type, map));
        }
        return out;
    }

    private static RewardDef parseRewards(ConfigurationSection sec) {
        if (sec == null)
            return new RewardDef(null, null, 0, 0, null, null, null, null, null);

        // currencies
        Map<String, Integer> currencies = new HashMap<>();
        ConfigurationSection curSec = sec.getConfigurationSection("currencies");
        if (curSec != null) {
            for (String key : curSec.getKeys(false))
                currencies.put(key, curSec.getInt(key, 0));
        }

        // jobXp
        Map<String, Integer> jobXp = new HashMap<>();
        ConfigurationSection jobSec = sec.getConfigurationSection("jobXp");
        if (jobSec != null) {
            for (String key : jobSec.getKeys(false))
                jobXp.put(key, jobSec.getInt(key, 0));
        }

        int housingXp = sec.getInt("housingXp", 0);
        int rankXp = sec.getInt("rankXp", 0);

        List<Map<String, Object>> crates = mapList(sec.getMapList("crates"));
        List<Map<String, Object>> items = mapList(sec.getMapList("items"));
        List<Map<String, Object>> cosmetics = mapList(sec.getMapList("cosmetics"));

        Map<String, Boolean> flags = new HashMap<>();
        ConfigurationSection flagSec = sec.getConfigurationSection("flags");
        if (flagSec != null) {
            for (String key : flagSec.getKeys(false))
                flags.put(key, flagSec.getBoolean(key, true));
        }

        List<ActionDef> extraActions = List.of();
        Object extraObj = sec.get("extraActions");
        if (extraObj instanceof List<?> raw)
            extraActions = parseActions(raw);

        return new RewardDef(currencies, jobXp, housingXp, rankXp,
                crates, items, cosmetics, flags, extraActions);
    }

    /** Parses quest timing from the given configuration section. */
    private static QuestTiming parseTiming(ConfigurationSection sec) {
        if (sec == null)
            return new QuestTiming(null, null, false);

        Long expires = sec.contains("expiresAfterSeconds")
                ? sec.getLong("expiresAfterSeconds")
                : null;
        Long cooldown = sec.contains("cooldownSeconds")
                ? sec.getLong("cooldownSeconds")
                : null;
        boolean hardFail = sec.getBoolean("hardFailOnExpire", false);

        return new QuestTiming(expires, cooldown, hardFail);
    }

    /** Converts a list of raw maps to a list of maps with String keys. */
    private static List<Map<String, Object>> mapList(List<Map<?, ?>> list) {
        if (list == null)
            return List.of();
        List<Map<String, Object>> out = new ArrayList<>();

        for (Map<?, ?> raw : list) {
            Map<String, Object> m = new HashMap<>();
            raw.forEach((k, v) -> m.put(String.valueOf(k), v));
            out.add(m);
        }
        return out;
    }
}
