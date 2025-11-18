package dev.chrona.common.dialogue;

import dev.chrona.common.dialogue.actions.ActionDef;
import dev.chrona.common.dialogue.conditions.ConditionDef;

import java.util.List;
import java.util.Map;

public final class DialogueNode {
    private String id;
    private Speaker speaker;
    private String npcName;
    private Map<String, List<String>> text;
    private List<ConditionDef> conditions;
    private List<ActionDef> enterActions;
    private List<ActionDef> exitActions;
    private List<DialogueChoice> choices;
    private boolean end;

    /**
     * Gets the unique identifier for this dialogue node.
     *
     * @return The ID of the dialogue node.
     */
    public String getId() { return id; }

    /**
     * Gets the speaker type for this dialogue node.
     *
     * @return The Speaker enum value representing the speaker.
     */
    public Speaker getSpeaker() { return speaker; }

    /**
     * Gets the name of the NPC associated with this dialogue node, if applicable.
     *
     * @return The NPC name as a string.
     */
    public String getNpcName() { return npcName; }

    /**
     * Gets the localized text for this dialogue node.
     *
     * @return A map of language codes to lists of localized text strings.
     */
    public Map<String, List<String>> getText() { return text; }

    /**
     * Gets the list of conditions that must be met to enter this dialogue node.
     *
     * @return A list of ConditionDef objects representing the conditions.
     */
    public List<ConditionDef> getConditions() { return conditions; }

    /**
     * Gets the list of actions to be executed upon entering this dialogue node.
     *
     * @return A list of ActionDef objects representing the enter actions.
     */
    public List<ActionDef> getEnterActions() { return enterActions; }

    /**
     * Gets the list of actions to be executed upon exiting this dialogue node.
     *
     * @return A list of ActionDef objects representing the exit actions.
     */
    public List<ActionDef> getExitActions() { return exitActions; }

    /**
     * Gets the list of dialogue choices available at this node.
     *
     * @return A list of DialogueChoice objects representing the choices.
     */
    public List<DialogueChoice> getChoices() { return choices; }

    /**
     * Indicates whether this dialogue node is an end node.
     *
     * @return True if this is an end node; otherwise, false.
     */
    public boolean isEnd() { return end; }
}

