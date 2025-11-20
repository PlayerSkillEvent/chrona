package dev.chrona.quest.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of an action to be performed as part of a quest.
 */
public final class ActionDef {

    private final ActionType type;
    private final Map<String, Object> params;

    public ActionDef(ActionType type, Map<String, Object> params) {
        this.type = Objects.requireNonNull(type, "type");
        this.params = params != null ? Map.copyOf(params) : Map.of();
    }

    /**
     * Gets the type of action.
     *
     * @return the action type
     */
    public ActionType type() {
        return type;
    }

    /**
     * Gets the parameters associated with the action.
     *
     * @return an unmodifiable map of parameters
     */
    public Map<String, Object> params() {
        return params;
    }

    /**
     * Retrieves a string parameter by key.
     *
     * @param key the parameter key
     * @return the string value, or null if not found
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

    /**
     * Creates a simple ActionDef with a single parameter.
     *
     * @param type  the action type
     * @param key   the parameter key
     * @param value the parameter value
     * @return a new ActionDef instance
     */
    public static ActionDef simple(ActionType type, String key, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return new ActionDef(type, m);
    }
}
