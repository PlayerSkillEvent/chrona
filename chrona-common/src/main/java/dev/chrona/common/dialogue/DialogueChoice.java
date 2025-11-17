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

    public String getId() { return id; }
    public Map<String, String> getText() { return text; }
    public List<ConditionDef> getConditions() { return conditions; }
    public List<ActionDef> getActions() { return actions; }
    public String getNext() { return next; }
}
