package dev.chrona.quest.condition;

import dev.chrona.quest.model.QuestDefinition;

import java.util.UUID;

/**
 * Creates ConditionContext instances for given player IDs and quest definitions.
 */
public interface ConditionContextFactory {

    /**
     * Creates a ConditionContext for the specified player and quest.
     *
     * @param playerId the unique identifier of the player
     * @param quest    the quest definition
     * @return a new ConditionContext instance
     */
    ConditionContext create(UUID playerId, QuestDefinition quest);
}
