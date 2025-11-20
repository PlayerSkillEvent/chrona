package dev.chrona.quest.engine;

import dev.chrona.quest.model.QuestDefinition;
import dev.chrona.quest.state.PlayerQuestState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main interface for the quest engine system.
 * Provides methods to manage quest definitions, player quest states,
 */
public interface QuestEngine {

    /**
     * Retrieves a quest definition by its unique identifier.
     *
     * @param questId The unique identifier of the quest.
     * @return An Optional containing the QuestDefinition if found, or empty if not found.
     */
    Optional<QuestDefinition> getDefinition(String questId);

    /**
     * Retrieves the current state of a player's quest, or creates a new state if none exists.
     *
     * @param playerId The unique identifier of the player.
     * @param questId  The unique identifier of the quest.
     * @return The PlayerQuestState for the specified player and quest.
     */
    PlayerQuestState getOrCreateState(UUID playerId, String questId);

    /**
     * Checks if a player can start a specific quest based on their current state and the quest definition.
     *
     * @param state The current state of the player's quest.
     * @param def   The definition of the quest to be started.
     * @return true if the player can start the quest, false otherwise.
     */
    boolean canStart(PlayerQuestState state, QuestDefinition def);

    /**
     * Starts a quest for a player, initializing their quest state.
     *
     * @param playerId The unique identifier of the player.
     * @param questId  The unique identifier of the quest to be started.
     * @param ctx      The context for the quest operation.
     * @return The initialized PlayerQuestState for the started quest.
     */
    PlayerQuestState startQuest(UUID playerId, String questId, QuestContext ctx);

    /**
     * Abandons an active quest for a player.
     *
     * @param playerId The unique identifier of the player.
     * @param questId  The unique identifier of the quest to be abandoned.
     * @param ctx      The context for the quest operation.
     */
    void abandonQuest(UUID playerId, String questId, QuestContext ctx);

    /**
     * Fails a quest for a player, marking it as failed.
     *
     * @param playerId The unique identifier of the player.
     * @param questId  The unique identifier of the quest to be failed.
     * @param reason   The reason for failing the quest.
     * @param ctx      The context for the quest operation.
     */
    void failQuest(UUID playerId, String questId, String reason, QuestContext ctx);

    /**
     * Completes a quest for a player, marking it as completed.
     *
     * @param playerId The unique identifier of the player.
     * @param questId  The unique identifier of the quest to be completed.
     * @param ctx      The context for the quest operation.
     */
    void completeQuest(UUID playerId, String questId, QuestContext ctx);

    /**
     * Applies progress to a specific objective of a player's quest.
     *
     * @param playerId    The unique identifier of the player.
     * @param questId     The unique identifier of the quest.
     * @param objectiveId The unique identifier of the objective.
     * @param increment   The amount to increment the objective's progress.
     * @param ctx         The context for the quest operation.
     */
    void applyObjectiveProgress(UUID playerId, String questId, String objectiveId,
                                long increment, QuestContext ctx);

    /**
     * Retrieves a list of all active quests for a specific player.
     *
     * @param playerId The unique identifier of the player.
     * @return A list of PlayerQuestState representing the player's active quests.
     */
    List<PlayerQuestState> getActiveQuests(UUID playerId);
}
