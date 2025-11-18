package dev.chrona.common.dialogue;

import dev.chrona.common.dialogue.conditions.ConditionDef;

import java.util.List;

/** Represents the binding of a dialogue to a specific NPC, including conditions for starting the dialogue. */
public final class NpcDialogueBinding {

    /** Defines the modes for starting a dialogue. */
    public enum StartMode { ON_INTERACT, NEVER }

    private String npcName;
    private String dialogueId;
    private StartMode startMode;
    private List<ConditionDef> conditions;

    /**
     * Gets the name of the NPC associated with this dialogue binding.
     *
     * @return The NPC name.
     */
    public String getNpcName() { return npcName; }

    /**
     * Gets the ID of the dialogue associated with this binding.
     *
     * @return The dialogue ID.
     */
    public String getDialogueId() { return dialogueId; }

    /**
     * Gets the mode for starting the dialogue.
     *
     * @return The start mode, defaulting to ON_INTERACT if not set.
     */
    public StartMode getStartMode() { return startMode != null ? startMode : StartMode.ON_INTERACT; }

    /**
     * Gets the list of conditions that must be met to start the dialogue.
     *
     * @return A list of ConditionDef objects representing the conditions.
     */
    public List<ConditionDef> getConditions() { return conditions; }
}
