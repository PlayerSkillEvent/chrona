package dev.chrona.common.dialogue;

import dev.chrona.common.dialogue.conditions.ConditionDef;

import java.util.List;

public final class NpcDialogueBinding {
    public enum StartMode { ON_INTERACT, NEVER }

    private String npcName;
    private String dialogueId;
    private StartMode startMode;
    private List<ConditionDef> conditions;

    public String getNpcName() { return npcName; }
    public String getDialogueId() { return dialogueId; }
    public StartMode getStartMode() { return startMode != null ? startMode : StartMode.ON_INTERACT; }
    public List<ConditionDef> getConditions() { return conditions; }
}
