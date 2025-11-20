package dev.chrona.quest.state;

/**
 * Represents the various states a quest run can be in.
 */
public enum QuestRunState {
    LOCKED,
    AVAILABLE,
    ACTIVE,
    COMPLETED,
    FAILED,
    ABANDONED,
    COOLDOWN
}
