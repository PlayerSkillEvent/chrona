package dev.chrona.quest.state;

import dev.chrona.quest.model.QuestType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a historical entry for a quest, capturing state changes and actions taken by a player.
 */
public final class QuestHistoryEntry {

    private final UUID playerId;
    private final String questId;
    private final QuestType type;
    private final String action;    // START/COMPLETE/FAIL/ABANDON/EXPIRE/RESET
    private final String fromState;
    private final String toState;
    private final String world;
    private final Integer x;
    private final Integer y;
    private final Integer z;
    private final Map<String, Object> extra;
    private final Instant createdAt;

    public QuestHistoryEntry(UUID playerId, String questId, QuestType type,
                             String action, String fromState, String toState,
                             String world, Integer x, Integer y, Integer z,
                             Map<String, Object> extra, Instant createdAt) {
        this.playerId = playerId;
        this.questId = questId;
        this.type = type;
        this.action = action;
        this.fromState = fromState;
        this.toState = toState;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.extra = extra;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**     * Returns the player ID associated with this quest history entry.
     *
     * @return the player ID
     */
    public UUID playerId() { return playerId; }

    /**     * Returns the quest ID associated with this quest history entry.
     *
     * @return the quest ID
     */
    public String questId() { return questId; }

    /**     * Returns the type of the quest.
     *
     * @return the quest type
     */
    public QuestType type() { return type; }

    /**     * Returns the action taken on the quest.
     *
     * @return the action
     */
    public String action() { return action; }

    /**     * Returns the previous state of the quest.
     *
     * @return the from state
     */
    public String fromState() { return fromState; }

    /**     * Returns the new state of the quest.
     *
     * @return the to state
     */
    public String toState() { return toState; }

    /**     * Returns the world where the action took place.
     *
     * @return the world
     */
    public String world() { return world; }

    /**     * Returns the X coordinate where the action took place.
     *
     * @return the X coordinate
     */
    public Integer x() { return x; }

    /**     * Returns the Y coordinate where the action took place.
     *
     * @return the Y coordinate
     */
    public Integer y() { return y; }

    /**     * Returns the Z coordinate where the action took place.
     *
     * @return the Z coordinate
     */
    public Integer z() { return z; }

    /**     * Returns any extra data associated with this quest history entry.
     *
     * @return the extra data map
     */
    public Map<String, Object> extra() { return extra; }

    /**     * Returns the timestamp when this quest history entry was created.
     *
     * @return the creation timestamp
     */
    public Instant createdAt() { return createdAt; }
}
