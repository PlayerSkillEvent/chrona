package dev.chrona.quest.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents the logical conditions for quest requirements.
 * It includes three types of conditions:
 * - allOf: All conditions in this list must be met.
 * - anyOf: At least one condition in this list must be met.
 * - noneOf: No conditions in this list should be met.
 */
public final class ConditionLogic {

    private final List<ConditionDef> allOf;
    private final List<ConditionDef> anyOf;
    private final List<ConditionDef> noneOf;

    public ConditionLogic(List<ConditionDef> allOf,
                          List<ConditionDef> anyOf,
                          List<ConditionDef> noneOf) {
        this.allOf = allOf != null ? List.copyOf(allOf) : List.of();
        this.anyOf = anyOf != null ? List.copyOf(anyOf) : List.of();
        this.noneOf = noneOf != null ? List.copyOf(noneOf) : List.of();
    }

    /**
     * Returns an unmodifiable list of conditions that must all be met.
     *
     * @return the list of allOf conditions
     */
    public List<ConditionDef> allOf() {
        return allOf;
    }

    /**
     * Returns an unmodifiable list of conditions where at least one must be met.
     *
     * @return the list of anyOf conditions
     */
    public List<ConditionDef> anyOf() {
        return anyOf;
    }

    /**
     * Returns an unmodifiable list of conditions that must not be met.
     *
     * @return the list of noneOf conditions
     */
    public List<ConditionDef> noneOf() {
        return noneOf;
    }
}
