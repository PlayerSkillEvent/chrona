package dev.chrona.job.core;

import dev.chrona.job.api.Job;
import dev.chrona.job.jobs.miner.MinerJob;
import java.util.Map;

public final class Jobs {
    private Jobs() {}
    public static Map<String, Job> available() {
        return Map.of(
                "MINER", new MinerJob()
        );
    }
}
