package dev.chrona.job.api;

import dev.chrona.job.core.JobContext;
import dev.chrona.job.core.JobRuntime;
import org.bukkit.event.Listener;
import java.util.Collection;

public interface Job {
    String id();
    String displayName();
    void onEnable(JobContext ctx);
    void onDisable();
    Collection<Listener> listeners(JobRuntime runtime);
}
