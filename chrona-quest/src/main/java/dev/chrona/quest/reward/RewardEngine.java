package dev.chrona.quest.reward;

import dev.chrona.quest.action.ActionExecutionContext;
import dev.chrona.quest.model.QuestDefinition;
import dev.chrona.quest.model.RewardDef;

/**
 * Interface for applying rewards to quests.
 */
public interface RewardEngine {

    /**
     * Applies the specified rewards to the given quest within the provided context.
     *
     * @param rewardCtx    The reward context containing necessary information for applying rewards.
     * @param actionCtx   The action execution context relevant to the reward application.
     * @param quest  The quest definition to which rewards will be applied.
     * @param rewards The reward definitions to be applied to the quest.
     */
    void applyRewards(RewardContext rewardCtx, ActionExecutionContext actionCtx, QuestDefinition quest, RewardDef rewards);
}
