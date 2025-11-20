package dev.chrona.quest.state;

import dev.chrona.quest.model.QuestDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for managing the storage and retrieval of quest states and objective progress for players.
 */
public interface QuestStateStore {

    /**
     * Finds the quest state for a specific player and quest.
     *
     * @param playerId the unique identifier of the player
     * @param questId  the unique identifier of the quest
     * @return an Optional containing the PlayerQuestState if found, otherwise empty
     */
    Optional<PlayerQuestState> findState(UUID playerId, String questId);

    /**
     * Retrieves or creates a quest state for a specific player and quest definition.
     *
     * @param playerId the unique identifier of the player
     * @param def      the quest definition
     * @return the PlayerQuestState for the player and quest
     */
    PlayerQuestState getOrCreateState(UUID playerId, QuestDefinition def);

    /**
     * Saves the state of a player's quest.
     *
     * @param state the PlayerQuestState to be saved
     */
    void saveState(PlayerQuestState state);


    /**
     * Deletes the quest state for a specific player and quest.
     *
     * @param playerId the unique identifier of the player
     */
    List<PlayerQuestState> getStatesByPlayer(UUID playerId);


    /**
     * Retrieves all active quest states for a specific player.
     *
     * @param playerId the unique identifier of the player
     * @return a list of active PlayerQuestState objects
     */
    List<PlayerQuestState> getActiveStates(UUID playerId);


    /**
     * Retrieves or creates the progress of a specific objective for a player's quest.
     *
     * @param playerId    the unique identifier of the player
     * @param questId     the unique identifier of the quest
     * @param objectiveId the unique identifier of the objective
     * @return the ObjectiveProgress for the player, quest, and objective
     */
    ObjectiveProgress getOrCreateObjectiveProgress(UUID playerId, String questId, String objectiveId);


    /**
     * Saves the progress of a player's objective.
     *
     * @param progress the ObjectiveProgress to be saved
     */
    void saveObjectiveProgress(ObjectiveProgress progress);

    /**
     * Retrieves all objective progress entries for a specific player's quest.
     *
     * @param playerId the unique identifier of the player
     * @param questId  the unique identifier of the quest
     * @return a list of ObjectiveProgress objects
     */
    List<ObjectiveProgress> getObjectiveProgress(UUID playerId, String questId);

    /**
     * Deletes the progress of a specific objective for a player's quest.
     *
     * @param playerId the unique identifier of the player
     * @param questId  the unique identifier of the quest
     */
    void deleteObjectiveProgress(UUID playerId, String questId);

    /**
     * Logs an entry in the quest history.
     *
     * @param entry the QuestHistoryEntry to be logged
     */
    void logHistory(QuestHistoryEntry entry);
}
