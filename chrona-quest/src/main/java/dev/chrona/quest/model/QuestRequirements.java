package dev.chrona.quest.model;

/**
 * Represents the requirements for a quest, encapsulating the conditions that must be met.
 */
public final class QuestRequirements {

    private final ConditionLogic requirements;

    public QuestRequirements(ConditionLogic requirements) {
        this.requirements = requirements != null ? requirements : new ConditionLogic(null, null, null);
    }

    /**
     * Returns the condition logic that defines the quest requirements.
     *
     * @return the condition logic
     */
    public ConditionLogic logic() {
        return requirements;
    }
}
