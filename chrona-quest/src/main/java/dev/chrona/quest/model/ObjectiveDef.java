package dev.chrona.quest.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of a quest objective.
 */
public final class ObjectiveDef {

    private final String id;
    private final ObjectiveType type;

    private final String title;
    private final String description;
    private final String trackerText;

    private final long target;
    private final String countMode; // "INCREMENT", "SET", "BOOL" – später ggf. Enum

    private final boolean autoComplete;
    private final boolean allowPartialProgress;

    private final ConditionLogic requirements;
    private final List<ActionDef> onStart;
    private final List<ActionDef> onComplete;

    // Specific fields (NPC, Region, Items, etc.) as generic params
    private final Map<String, Object> params;

    public ObjectiveDef(
            String id,
            ObjectiveType type,
            String title,
            String description,
            String trackerText,
            long target,
            String countMode,
            boolean autoComplete,
            boolean allowPartialProgress,
            ConditionLogic requirements,
            List<ActionDef> onStart,
            List<ActionDef> onComplete,
            Map<String, Object> params
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.title = title != null ? title : id;
        this.description = description;
        this.trackerText = trackerText;
        this.target = target > 0 ? target : 1;
        this.countMode = countMode != null ? countMode : "INCREMENT";
        this.autoComplete = autoComplete;
        this.allowPartialProgress = allowPartialProgress;
        this.requirements = requirements != null ? requirements : new ConditionLogic(null, null, null);
        this.onStart = onStart != null ? List.copyOf(onStart) : List.of();
        this.onComplete = onComplete != null ? List.copyOf(onComplete) : List.of();
        this.params = params != null ? Map.copyOf(params) : Map.of();
    }

    /** Returns the unique identifier of the objective.
     *
     * @return the objective ID
     */
    public String id() {
        return id;
    }

    /** Returns the type of the objective.
     *
     * @return the objective type
     */
    public ObjectiveType type() {
        return type;
    }

    /** Returns the title of the objective.
     *
     * @return the objective title
     */
    public String title() {
        return title;
    }

    /** Returns the description of the objective.
     *
     * @return the objective description
     */
    public String description() {
        return description;
    }

    /** Returns the tracker text of the objective.
     *
     * @return the objective tracker text
     */
    public String trackerText() {
        return trackerText;
    }

    /** Returns the target count for the objective.
     *
     * @return the target count
     */
    public long target() {
        return target;
    }

    /** Returns the count mode of the objective.
     *
     * @return the count mode
     */
    public String countMode() {
        return countMode;
    }

    /** Indicates whether the objective should auto-complete.
     *
     * @return true if auto-complete is enabled, false otherwise
     */
    public boolean autoComplete() {
        return autoComplete;
    }

    /** Indicates whether partial progress is allowed for the objective.
     *
     * @return true if partial progress is allowed, false otherwise
     */
    public boolean allowPartialProgress() {
        return allowPartialProgress;
    }

    /** Returns the requirements logic for the objective.
     *
     * @return the requirements logic
     */
    public ConditionLogic requirements() {
        return requirements;
    }

    /** Returns the list of actions to perform on objective start.
     *
     * @return the onStart actions
     */
    public List<ActionDef> onStart() {
        return onStart;
    }

    /** Returns the list of actions to perform on objective completion.
     *
     * @return the onComplete actions
     */
    public List<ActionDef> onComplete() {
        return onComplete;
    }

    /** Returns an unmodifiable view of the parameters map.
     *
     * @return the parameters map
     */
    public Map<String, Object> params() {
        return params;
    }

    /** Retrieves a parameter as a String.
     *
     * @param key the parameter key
     * @return the parameter value as a String, or null if not found
     */
    public String getString(String key) {
        Object o = params.get(key);
        return o != null ? String.valueOf(o) : null;
    }

    /** Retrieves an integer parameter by key.
     *
     * @param key the parameter key
     * @return the integer value, or null if not found or not parsable
     */
    public Integer getInt(String key) {
        Object o = params.get(key);
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

    /** Retrieves a long parameter by key.
     *
     * @param key the parameter key
     * @return the long value, or null if not found or not parsable
     */
    public Long getLong(String key) {
        Object o = params.get(key);
        if (o instanceof Number n)
            return n.longValue();
        if (o == null)
            return null;
        try {
            return Long.parseLong(String.valueOf(o));
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /** Retrieves a boolean parameter by key.
     *
     * @param key the parameter key
     * @return the boolean value, or null if not found
     */
    public Boolean getBool(String key) {
        Object o = params.get(key);
        if (o instanceof Boolean b)
            return b;
        if (o == null)
            return null;

        return Boolean.parseBoolean(String.valueOf(o));
    }
}
