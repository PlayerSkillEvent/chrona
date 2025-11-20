package dev.chrona.quest.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of a condition with a type and parameters.
 */
public final class ConditionDef {

    private final ConditionType type;
    private final Map<String, Object> params;

    public ConditionDef(ConditionType type, Map<String, Object> params) {
        this.type = Objects.requireNonNull(type, "type");
        this.params = params != null
                ? Map.copyOf(params)
                : Map.of();
    }

    /**
     * Returns the type of the condition.
     *
     * @return the condition type
     */
    public ConditionType type() {
        return type;
    }

    /**
     * Returns an unmodifiable view of the parameters map.
     *
     * @return the parameters map
     */
    public Map<String, Object> params() {
        return params;
    }

    /**
     * Retrieves a parameter as a String.
     *
     * @param key the parameter key
     * @return the parameter value as a String, or null if not found
     */
    public String getString(String key) {
        Object o = params.get(key);
        return o != null ? String.valueOf(o) : null;
    }

    /**
     * Retrieves an integer parameter by key.
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

    /**
     * Retrieves a boolean parameter by key.
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
