package dev.chrona.job.jobs.miner;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.job.api.Job;
import dev.chrona.job.core.JobConfig;
import dev.chrona.job.core.JobContext;
import dev.chrona.job.core.JobPlayerState;
import dev.chrona.job.core.JobRuntime;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.slf4j.Logger;

import java.util.*;

public final class MinerJob implements Job {
    private Map<String, Long> rewards;
    private long perLevel;
    private String display;
    private Logger logger;

    @Override
    public String id() {
        return "MINER";
    }

    @Override
    public String displayName() {
        return display != null ? display : "Miner";
    }

    @Override
    public void onEnable(JobContext ctx) {
        logger = ChronaLog.get(MinerJob.class);
        JobConfig cfg = ctx.config().jobConfig("MINER");
        this.rewards = cfg.getMapLong("rewards");
        this.perLevel = cfg.getLong("levelBonus.basePerLevel", 0);
        this.display = cfg.getString("display", "Miner");

        logger.info("MinerJob enabled with {} reward entries and {} per-level bonus.", rewards.size(), perLevel);
    }

    @Override
    public void onDisable() {}

    @Override
    public Collection<Listener> listeners(JobRuntime runtime) {
        return List.of(new MinerListener(runtime, this));
    }

    long rewardFor(Material m, int level) {
        long base = rewards.getOrDefault(m.name(), 0L);
        return Math.max(0, base + perLevel * Math.max(0, level - 1));
    }

    static final class MinerListener implements Listener {
        private final JobRuntime runtime; private final MinerJob job;
        MinerListener(JobRuntime r, MinerJob j) { this.runtime = r; this.job = j; }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onBreak(BlockBreakEvent e) {
            var p = e.getPlayer();
            var m = e.getBlock().getType();
            int level = runtime.getState(p.getUniqueId(), "MINER").map(JobPlayerState::level).orElse(1);
            long amt = job.rewardFor(m, level);
            if (amt <= 0) return;

            Bukkit.getScheduler().runTaskAsynchronously(
                    Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Chrona")),
                    () -> runtime.reward(p.getUniqueId(), "MINER", amt, Map.of("block", m.name()))
            );
        }
    }
}
