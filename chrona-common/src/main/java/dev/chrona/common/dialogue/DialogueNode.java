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

    public String getId() { return id; }
    public Speaker getSpeaker() { return speaker; }
    public String getNpcName() { return npcName; }
    public Map<String, List<String>> getText() { return text; }
    public List<ConditionDef> getConditions() { return conditions; }
    public List<ActionDef> getEnterActions() { return enterActions; }
    public List<ActionDef> getExitActions() { return exitActions; }
    public List<DialogueChoice> getChoices() { return choices; }
    public boolean isEnd() { return end; }
}

