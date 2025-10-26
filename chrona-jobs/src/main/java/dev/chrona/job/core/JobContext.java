package dev.chrona.job.core;

import dev.chrona.common.hologram.protocol.ProtocolHolograms;
import dev.chrona.economy.EconomyService;
import dev.chrona.minigames.core.MinigameManager;
import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;

public record JobContext(Plugin plugin, DataSource ds, EconomyService econ, MinigameManager minigames, ProtocolHolograms holo, JobConfigProvider config) {}
