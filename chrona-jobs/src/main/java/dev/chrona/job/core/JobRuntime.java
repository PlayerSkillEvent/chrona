package dev.chrona.job.core;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JobRuntime {

    /**
     * Gives a reward to a player for a specific job.
     *
     * @param playerId Player UUID, which shall recieve the reward
     * @param jobId    Job ID (e.g. key or name)
     * @param amount   Amount of the reward
     * @param payload  additional, optional metadata; can contain key/value-pairs
     */
    void reward(UUID playerId, String jobId, long amount, Map<String,Object> payload);

    /**
     * Returns the current state of the player for this job.
     *
     * @param playerId Player UUID
     * @param jobId    Job ID
     * @return Optional with the JobPlayerState, if available; empty, if no state was found
     */
    Optional<JobPlayerState> getState(UUID playerId, String jobId);
}
