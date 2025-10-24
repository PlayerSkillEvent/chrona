package dev.chrona.plugin;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.log.LoggingBootstrap;
import dev.chrona.economy.PgEconomy;
import dev.chrona.economy.PlayerRepo;
import dev.chrona.job.core.*;
import dev.chrona.plugin.commands.EconCmd;
import dev.chrona.plugin.commands.PayCmd;
import dev.chrona.plugin.commands.WalletCmd;
import dev.chrona.plugin.listeners.JoinListener;
import dev.chrona.minigames.Minigames;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import dev.chrona.common.Db;

import javax.sql.DataSource;
import java.util.Objects;

public final class ChronaPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Db.migrate(this.getClassLoader(), "classpath:db/migration");
        LoggingBootstrap.init(getDataFolder().toPath(), true);
        DataSource ds = getDs();
        var logger = ChronaLog.get(ChronaPlugin.class);

        var econ = new PgEconomy(ds);
        var playerRepo = new PlayerRepo(ds);

        registerCommand("wallet", new WalletCmd(econ));
        registerCommand("pay", new PayCmd(econ));
        registerCommand("econ", new EconCmd(econ));

        registerEvent(new JoinListener(this, playerRepo));

        String season = getConfig().getString("season", "S1");
        JobConfigProvider cfgProvider = new ClasspathSeasonConfigProvider(season, getClassLoader(), getDataFolder().toPath());
        JobContext ctx = new JobContext(ds, econ, cfgProvider);
        JobRuntime runtime = new JobRewardRuntime(econ, ds, () -> season);

        var enabled = getConfig().getStringList("jobs.enabled");
        var all = Jobs.available();
        enabled.stream().map(String::toUpperCase).forEach(id -> {
            var job = all.get(id);
            if (job == null) {
                logger.warn("Unknown job in config: {}", id);
                return; }
            job.onEnable(ctx);
            job.listeners(runtime).forEach(l -> getServer().getPluginManager().registerEvents(l, this));
        });

       Minigames.init(this);


        logger.info("Chrona up.");
    }

    private static DataSource getDs() {
        return Db.ds();
    }

    private void registerEvent(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        Db.close();
    }

    private void registerCommand(String cmd, CommandExecutor executor) {
        Objects.requireNonNull(getCommand(cmd)).setExecutor(executor);
    }
}
