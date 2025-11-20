package dev.chrona.quest.state;

import dev.chrona.quest.model.QuestRepeatability;
import dev.chrona.quest.model.QuestType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the state of a player's quest, including progress, timestamps, and statistics.
 */
public final class PlayerQuestState {

    private final UUID playerId;
    private final String questId;

    private final QuestType type;
    private final QuestRepeatability repeatability;

    private QuestRunState state;

    private Integer currentObjectiveIndex;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant lastUpdatedAt;
    private Instant expiresAt;
    private Instant nextAvailableAt;

    private int timesCompleted;
    private int timesFailed;
    private String lastResult; // "COMPLETE" / "FAIL" / etc.

    public PlayerQuestState(UUID playerId,
                            String questId,
                            QuestType type,
                            QuestRepeatability repeatability,
                            QuestRunState state,
                            Integer currentObjectiveIndex,
                            Instant startedAt,
                            Instant completedAt,
                            Instant failedAt,
                            Instant lastUpdatedAt,
                            Instant expiresAt,
                            Instant nextAvailableAt,
                            int timesCompleted,
                            int timesFailed,
                            String lastResult) {

        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.questId = Objects.requireNonNull(questId, "questId");
        this.type = Objects.requireNonNull(type, "type");
        this.repeatability = Objects.requireNonNull(repeatability, "repeatability");
        this.state = Objects.requireNonNull(state, "state");
        this.currentObjectiveIndex = currentObjectiveIndex;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : Instant.now();
        this.expiresAt = expiresAt;
        this.nextAvailableAt = nextAvailableAt;
        this.timesCompleted = timesCompleted;
        this.timesFailed = timesFailed;
        this.lastResult = lastResult;
    }

    /**
     * Returns the player's unique identifier.
     *
     * @return the player ID
     */
    public UUID playerId() { return playerId; }

    /**
     * Returns the quest's unique identifier.
     *
     * @return the quest ID
     */
    public String questId() { return questId; }

    /**
     * Returns the type of the quest.
     *
     * @return the quest type
     */
    public QuestType type() { return type; }

    /**
     * Returns the repeatability of the quest.
     *
     * @return the quest repeatability
     */
    public QuestRepeatability repeatability() { return repeatability; }

    /**
     * Returns the current run state of the quest.
     *
     * @return the quest run state
     */
    public QuestRunState state() { return state; }

    /**
     * Sets the current run state of the quest.
     *
     * @param state the new quest run state
     */
    public void setState(QuestRunState state) {
        this.state = state;
        touch();
    }

    /**
     * Returns the index of the current objective in the quest.
     *
     * @return the current objective index
     */
    public Integer currentObjectiveIndex() { return currentObjectiveIndex; }

    /**
     * Sets the index of the current objective in the quest.
     *
     * @param idx the new current objective index
     */
    public void setCurrentObjectiveIndex(Integer idx) {
        this.currentObjectiveIndex = idx;
        touch();
    }


    /**
     * Returns the timestamp when the quest was started.
     *
     * @return the start timestamp
     */
    public Instant startedAt() { return startedAt; }

    /**
     * Sets the timestamp when the quest was started.
     *
     * @param startedAt the new start timestamp
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
        touch();
    }

    /**
     * Returns the timestamp when the quest was completed.
     *
     * @return the completion timestamp
     */
    public Instant completedAt() { return completedAt; }

    /**
     * Sets the timestamp when the quest was completed.
     *
     * @param completedAt the new completion timestamp
     */
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        touch();
    }

    /**
     * Returns the timestamp when the quest was failed.
     *
     * @return the failure timestamp
     */
    public Instant failedAt() { return failedAt; }

    /**
     * Sets the timestamp when the quest was failed.
     *
     * @param failedAt the new failure timestamp
     */
    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        touch();
    }

    /**
     * Returns the timestamp when the quest state was last updated.
     *
     * @return the last updated timestamp
     */
    public Instant lastUpdatedAt() { return lastUpdatedAt; }

    /**
     * Returns the expiration timestamp of the quest.
     *
     * @return the expiration timestamp
     */
    public Instant expiresAt() { return expiresAt; }

    /**
     * Sets the expiration timestamp of the quest.
     *
     * @param expiresAt the new expiration timestamp
     */
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        touch();
    }

    /**
     * Returns the next available timestamp for the quest.
     *
     * @return the next available timestamp
     */
    public Instant nextAvailableAt() { return nextAvailableAt; }

    /**
     * Sets the next available timestamp for the quest.
     *
     * @param nextAvailableAt the new next available timestamp
     */
    public void setNextAvailableAt(Instant nextAvailableAt) {
        this.nextAvailableAt = nextAvailableAt;
        touch();
    }

    /**
     * Returns the number of times the quest has been completed.
     *
     * @return the completion count
     */
    public int timesCompleted() { return timesCompleted; }

    /**
     * Increments the completion count of the quest by one.
     */
    public void incTimesCompleted() {
        this.timesCompleted++;
        this.lastResult = "COMPLETE";
        touch();
    }

    /**
     * Returns the number of times the quest has failed.
     *
     * @return the failure count
     */
    public int timesFailed() { return timesFailed; }

    /**
     * Increments the failure count of the quest by one.
     */
    public void incTimesFailed() {
        this.timesFailed++;
        this.lastResult = "FAIL";
        touch();
    }

    /**
     * Returns the result of the last quest attempt.
     *
     * @return the last result
     */
    public String lastResult() { return lastResult; }

    /**
     * Updates the last updated timestamp to the current time.
     */
    private void touch() {
        this.lastUpdatedAt = Instant.now();
    }
}
