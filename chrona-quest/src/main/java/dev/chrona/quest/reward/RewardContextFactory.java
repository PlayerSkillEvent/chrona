package dev.chrona.quest.reward;

import dev.chrona.quest.model.QuestDefinition;

import java.util.UUID;

/**
 * Factory interface for creating RewardContext instances.
 */
public interface RewardContextFactory {

    /**
     * Creates a RewardContext for the specified player and quest.
     *
     * @param playerId the unique identifier of the player
     * @param quest    the quest definition
     * @return a new RewardContext instance
     */
    RewardContext create(UUID playerId, QuestDefinition quest);
}
