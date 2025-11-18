package dev.chrona.common.dialogue;

import dev.chrona.common.dialogue.actions.ActionRegistry;
import dev.chrona.common.dialogue.actions.RunCommandAction;
import dev.chrona.common.dialogue.actions.SetFlagAction;
import dev.chrona.common.dialogue.actions.StartQuestAction;
import dev.chrona.common.dialogue.conditions.ConditionRegistry;
import dev.chrona.common.dialogue.conditions.FlagSetCondition;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.npc.api.NpcHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.*;

public final class DialogueService {
    private final Logger log = ChronaLog.get(DialogueService.class);
    private final Plugin plugin;
    private final DialogueRegistry dialogues;
    private final NpcDialogueRegistry npcBindings;
    private final ConditionRegistry conditions = new ConditionRegistry();
    private final ActionRegistry actions = new ActionRegistry();
    private final PlayerFlagStore flagStore;

    private final Map<UUID, DialogueSession> byPlayer = new HashMap<>();
    private final Map<String, DialogueSession> bySessionId = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public DialogueService(Plugin plugin) {
        this.plugin = plugin;
        var baseDir = plugin.getDataFolder().toPath().resolve("dialogue");
        this.flagStore = new PlayerFlagStore(baseDir);
        this.dialogues = new DialogueRegistry(baseDir);
        this.npcBindings = new NpcDialogueRegistry(plugin, baseDir);

        registerDefaults();
    }

    /* Registers the default dialogue conditions and actions. */
    private void registerDefaults() {
        conditions.register(new FlagSetCondition(flagStore, true));
        conditions.register(new FlagSetCondition(flagStore, false));
        actions.register(new SetFlagAction(flagStore));
        actions.register(new StartQuestAction());
        actions.register(new RunCommandAction());
    }

    /** Reloads all dialogue data from disk. */
    public void reload() {
        dialogues.reload();
        npcBindings.reload();
    }

    /** Handles player interaction with an NPC. */
    public void handleNpcInteract(Player player, NpcHandle npc) {
        // if player already in dialogue: ignore / oder später "continue"
        if (byPlayer.containsKey(player.getUniqueId()))
            return;

        List<NpcDialogueBinding> bindings = npcBindings.getBindings(npc.name());
        for (NpcDialogueBinding b : bindings) {
            if (b.getStartMode() != NpcDialogueBinding.StartMode.ON_INTERACT)
                continue;
            if (!conditions.evaluateAll(player, npc, null, b.getConditions()))
                continue;

            startDialogue(player, npc, b.getDialogueId());
            return;
        }
    }

    /** Starts a dialogue for the given player and NPC. */
    public void startDialogue(Player player, NpcHandle npc, String dialogueId) {
        Dialogue d = dialogues.get(dialogueId);
        if (d == null) {
            log.warn("Dialogue '{}' not found.", dialogueId);
            return;
        }
        String startNode = d.getStartNode();
        if (startNode == null || d.getNode(startNode) == null) {
            log.warn("Dialogue '{}' has invalid startNode '{}'", dialogueId, startNode);
            return;
        }

        String sessionId = randomId();
        DialogueSession session = new DialogueSession(player.getUniqueId(), dialogueId, sessionId, npc, startNode);
        byPlayer.put(player.getUniqueId(), session);
        bySessionId.put(sessionId, session);

        showCurrentNode(player, session);
    }

    /** Generates a random session ID. */
    private String randomId() {
        byte[] bytes = new byte[5];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int val = b & 0xff;
            sb.append(Integer.toHexString(val), 0, 2);
        }
        return sb.toString();
    }

    /** Shows the current dialogue node to the player. */
    public void showCurrentNode(Player player, DialogueSession session) {
        Dialogue d = dialogues.get(session.getDialogueId());
        if (d == null) {
            endSession(player.getUniqueId());
            return;
        }
        DialogueNode node = d.getNode(session.getCurrentNodeId());
        if (node == null) {
            endSession(player.getUniqueId());
            return;
        }

        if (!conditions.evaluateAll(player, session.getNpc(), session, node.getConditions())) {
            endSession(player.getUniqueId());
            return;
        }

        // Enter Actions
        actions.executeAll(player, session.getNpc(), session, node.getEnterActions());

        // Clear screen feeling
        player.sendMessage(" ");

        // Speaker head
        String speakerName = switch (node.getSpeaker()) {
            case NPC -> node.getNpcName() != null ? node.getNpcName() : d.getTitle();
            case PLAYER -> player.getName();
            case SYSTEM -> "System";
        };

        player.sendMessage("§6" + speakerName + " §8»");

        // Text (multi-lang)
        List<String> lines = DialogueTextUtil.resolveText(node.getText(), player);
        if (lines != null) {
            for (String line : lines)
                player.sendMessage("§f" + line);
        }

        // End?
        if (node.isEnd() || node.getChoices() == null || node.getChoices().isEmpty()) {
            actions.executeAll(player, session.getNpc(), session, node.getExitActions());
            endSession(player.getUniqueId());
            return;
        }

        // Choices
        player.sendMessage(" ");
        player.sendMessage("§7Choose:");

        int idx = 1;
        for (DialogueChoice choice : node.getChoices()) {
            if (!conditions.evaluateAll(player, session.getNpc(), session, choice.getConditions()))
                continue;

            String label = DialogueTextUtil.resolveChoiceText(choice.getText(), player);
            if (label == null)
                continue;

            Component line = Component.text(idx + ") ", NamedTextColor.GOLD)
                    .append(Component.text(label, NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/dialogue choose " + session.getSessionId() + " " + choice.getId()));

            player.sendMessage(line);
            idx++;
        }

        if (idx == 1) { // keine sichtbare Choice
            actions.executeAll(player, session.getNpc(), session, node.getExitActions());
            endSession(player.getUniqueId());
        }
    }

    /** Handles the player's choice selection. */
    public void choose(Player player, String sessionId, String choiceId) {
        DialogueSession session = bySessionId.get(sessionId);
        if (session == null || !session.getPlayerId().equals(player.getUniqueId())) {
            player.sendMessage("§cThis dialogue is no longer active.");
            return;
        }

        Dialogue d = dialogues.get(session.getDialogueId());
        if (d == null) {
            endSession(player.getUniqueId());
            return;
        }
        DialogueNode node = d.getNode(session.getCurrentNodeId());
        if (node == null || node.getChoices() == null) {
            endSession(player.getUniqueId());
            return;
        }

        DialogueChoice selected = null;
        for (DialogueChoice c : node.getChoices()) {
            if (!conditions.evaluateAll(player, session.getNpc(), session, c.getConditions()))
                continue;
            if (c.getId().equalsIgnoreCase(choiceId)) {
                selected = c;
                break;
            }
        }
        if (selected == null) {
            player.sendMessage("§cThat option is not available.");
            return;
        }

        actions.executeAll(player, session.getNpc(), session, selected.getActions());
        actions.executeAll(player, session.getNpc(), session, node.getExitActions());

        String next = selected.getNext();
        if (next == null) {
            endSession(player.getUniqueId());
            return;
        }

        DialogueNode nextNode = d.getNode(next);
        if (nextNode == null) {
            endSession(player.getUniqueId());
            return;
        }

        session.setCurrentNodeId(next);
        showCurrentNode(player, session);
    }

    /** Ends the dialogue session for the given player ID. */
    public void endSession(UUID playerId) {
        DialogueSession session = byPlayer.remove(playerId);
        if (session != null)
            bySessionId.remove(session.getSessionId());
    }

    /** Ends the dialogue session for the given player. */
    public void endSessionFor(Player player) {
        endSession(player.getUniqueId());
    }
}

