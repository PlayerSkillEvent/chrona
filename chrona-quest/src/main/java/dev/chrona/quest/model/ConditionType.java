package dev.chrona.quest.model;

/**
 * Types of conditions that can be used to determine if a quest can be started or completed.
 */
public enum ConditionType {
    FLAG,
    QUEST_STATE,
    JOB_LEVEL,
    JOB_ACTIVE,
    HOUSING_LEVEL,
    PLAYER_RANK,
    REGION_VISITED,
    REGION_INSIDE,
    WORLD_STATE,
    EVENT_ACTIVE,
    TIME_RANGE
}
