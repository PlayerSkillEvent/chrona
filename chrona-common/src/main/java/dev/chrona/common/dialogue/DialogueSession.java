package dev.chrona.common.dialogue;

import dev.chrona.common.npc.api.NpcHandle;

import java.util.UUID;

/** Represents an active dialogue session between a player and an NPC. */
public final class DialogueSession {
    private final UUID playerId;
    private final String dialogueId;
    private final String sessionId;
    private final NpcHandle npc;
    private String currentNodeId;
    private long lastInteraction;

    public DialogueSession(UUID playerId, String dialogueId, String sessionId, NpcHandle npc, String startNode) {
        this.playerId = playerId;
        this.dialogueId = dialogueId;
        this.sessionId = sessionId;
        this.npc = npc;
        this.currentNodeId = startNode;
        this.lastInteraction = System.currentTimeMillis();
    }

    /** Returns the unique ID of the player in this session. */
    public UUID getPlayerId() { return playerId; }

    /** Returns the ID of the dialogue being used in this session. */
    public String getDialogueId() { return dialogueId; }

    /** Returns the unique session ID. */
    public String getSessionId() { return sessionId; }

    /** Returns the NPC involved in this session. */
    public NpcHandle getNpc() { return npc; }

    /** Returns the current node ID in the dialogue. */
    public String getCurrentNodeId() { return currentNodeId; }

    /** Sets the current node ID in the dialogue. */
    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
        touch();
    }

    /** Returns the timestamp of the last interaction in milliseconds. */
    public long getLastInteraction() { return lastInteraction; }

    /** Updates the last interaction timestamp to the current time. */
    public void touch() { this.lastInteraction = System.currentTimeMillis(); }
}
