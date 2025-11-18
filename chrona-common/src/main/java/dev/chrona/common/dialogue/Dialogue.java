package dev.chrona.common.dialogue;

import java.util.Map;

public final class Dialogue {
    private String id;
    private String title;
    private String startNode;
    private Map<String, DialogueNode> nodes;

    /**
     * Gets the unique identifier of the dialogue.
     *
     * @return The dialogue ID.
     */
    public String getId() { return id; }

    /**
     * Gets the title of the dialogue.
     *
     * @return The dialogue title.
     */
    public String getTitle() { return title; }

    /**
     * Gets the starting node ID of the dialogue.
     *
     * @return The start node ID.
     */
    public String getStartNode() { return startNode; }

    /**
     * Gets the map of dialogue nodes in the dialogue.
     *
     * @return A map of node IDs to DialogueNode objects.
     */
    public Map<String, DialogueNode> getNodes() { return nodes; }

    /**
     * Retrieves a specific dialogue node by its ID.
     *
     * @param nodeId The ID of the dialogue node to retrieve.
     * @return The DialogueNode object if found; otherwise, null.
     */
    public DialogueNode getNode(String nodeId) {
        if (nodes == null)
            return null;

        return nodes.get(nodeId);
    }
}
