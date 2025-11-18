package dev.chrona.common.dialogue;

import dev.chrona.common.dialogue.actions.ActionDef;
import dev.chrona.common.dialogue.conditions.ConditionDef;

import java.util.List;
import java.util.Map;

public final class DialogueChoice {
    private String id;
    private Map<String, String> text;
    private List<ConditionDef> conditions;
    private List<ActionDef> actions;
    private String next;

    /**
     * Gets the unique identifier for this dialogue choice.
     *
     * @return The ID of the dialogue choice.
     */
    public String getId() { return id; }

    /**
     * Gets the localized text for this dialogue choice.
     *
     * @return A map of language codes to localized text strings.
     */
    public Map<String, String> getText() { return text; }

    /**
     * Gets the list of conditions that must be met for this choice to be available.
     *
     * @return A list of ConditionDef objects representing the conditions.
     */
    public List<ConditionDef> getConditions() { return conditions; }

    /**
     * Gets the list of actions to be executed when this choice is selected.
     *
     * @return A list of ActionDef objects representing the actions.
     */
    public List<ActionDef> getActions() { return actions; }

    /**
     * Gets the ID of the next dialogue node to navigate to after this choice is selected.
     *
     * @return The ID of the next dialogue node.
     */
    public String getNext() { return next; }
}
