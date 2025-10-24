package dev.chrona.job.core;

import java.util.Map;

public interface JobConfig {
    long getLong(String path, long def);
    Map<String, Long> getMapLong(String path);
    String getString(String path, String def);
}
