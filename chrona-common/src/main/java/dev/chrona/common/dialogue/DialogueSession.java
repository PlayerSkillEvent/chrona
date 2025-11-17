package dev.chrona.common.dialogue;

import dev.chrona.common.npc.api.NpcHandle;

import java.util.UUID;

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

    public UUID getPlayerId() { return playerId; }
    public String getDialogueId() { return dialogueId; }
    public String getSessionId() { return sessionId; }
    public NpcHandle getNpc() { return npc; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; touch(); }
    public long getLastInteraction() { return lastInteraction; }
    public void touch() { this.lastInteraction = System.currentTimeMillis(); }
}
