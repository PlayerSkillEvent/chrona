package dev.chrona.job.core;

public interface JobConfigProvider {
    JobConfig jobConfig(String jobId);
}
