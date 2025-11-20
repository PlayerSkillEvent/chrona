package dev.chrona.quest.action;

import dev.chrona.quest.engine.QuestContext;
import dev.chrona.quest.model.QuestDefinition;

import java.util.UUID;

/**
 * Factory interface for creating ActionExecutionContext instances.
 */
public interface ActionContextFactory {

    /**
     * Creates a new ActionExecutionContext for the specified player and quest.
     *
     * @param playerId     the unique identifier of the player
     * @param quest        the quest definition
     * @param questContext the context of the quest
     * @return a new ActionExecutionContext instance
     */
    ActionExecutionContext create(UUID playerId, QuestDefinition quest, QuestContext questContext);
}
