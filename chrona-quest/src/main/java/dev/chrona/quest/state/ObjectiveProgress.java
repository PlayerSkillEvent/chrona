package dev.chrona.quest.state;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the progress of a specific objective within a quest for a player.
 */
public final class ObjectiveProgress {

    private final UUID playerId;
    private final String questId;
    private final String objectiveId;
    private long progress;
    private boolean completed;
    private Instant lastUpdated;

    public ObjectiveProgress(UUID playerId,
                             String questId,
                             String objectiveId,
                             long progress,
                             boolean completed,
                             Instant lastUpdated) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.questId = Objects.requireNonNull(questId, "questId");
        this.objectiveId = Objects.requireNonNull(objectiveId, "objectiveId");
        this.progress = progress;
        this.completed = completed;
        this.lastUpdated = lastUpdated != null ? lastUpdated : Instant.now();
    }

    /**
     * Returns the unique identifier of the player.
     *
     * @return the player ID
     */
    public UUID playerId() { return playerId; }

    /**
     * Returns the unique identifier of the quest.
     *
     * @return the quest ID
     */
    public String questId() { return questId; }

    /**
     * Returns the unique identifier of the objective.
     *
     * @return the objective ID
     */
    public String objectiveId() { return objectiveId; }

    /**
     * Returns the current progress value of the objective.
     *
     * @return the progress value
     */
    public long progress() { return progress; }

    /**
     * Sets the current progress value of the objective.
     *
     * @param progress the new progress value
     */
    public void setProgress(long progress) {
        this.progress = progress;
        this.lastUpdated = Instant.now();
    }

    /**
     * Returns whether the objective is completed.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() { return completed; }

    /**
     * Sets the completion status of the objective.
     *
     * @param completed the new completion status
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.lastUpdated = Instant.now();
    }

    /**
     * Returns the timestamp of the last update to the objective progress.
     *
     * @return the last updated timestamp
     */
    public Instant lastUpdated() { return lastUpdated; }
}

