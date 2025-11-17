package dev.chrona.common.dialogue;

import java.util.Map;

public final class Dialogue {
    private String id;
    private String title;
    private String startNode;
    private Map<String, DialogueNode> nodes;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getStartNode() { return startNode; }
    public Map<String, DialogueNode> getNodes() { return nodes; }

    public DialogueNode getNode(String nodeId) {
        if (nodes == null) return null;
        return nodes.get(nodeId);
    }
}
