package dev.chrona.quest.condition;

import dev.chrona.quest.model.ConditionLogic;

/**
 * Engine responsible for evaluating conditions based on provided logic and context.
 */
public interface ConditionEngine {

    /**
     * Evaluates the given condition logic within the specified context.
     *
     * @param logic the condition logic to evaluate
     * @param ctx   the context in which to evaluate the conditions
     * @return true if the conditions are met, false otherwise
     */
    boolean evaluate(ConditionLogic logic, ConditionContext ctx);
}
