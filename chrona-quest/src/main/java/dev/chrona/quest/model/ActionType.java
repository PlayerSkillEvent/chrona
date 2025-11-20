package dev.chrona.quest.model;

/**
 * Represents the different types of actions that can be performed in a quest.
 */
public enum ActionType {
    SET_FLAG,
    CLEAR_FLAG,
    DIALOGUE_START,
    TELEPORT,
    MESSAGE,
    TITLE,
    SOUND,
    RUN_COMMAND,
    GIVE_ITEM,
    WORLD_STATE_SET,
    BROADCAST
}
