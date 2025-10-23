package dev.chrona.plugin;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.plugin.commands.BalanceCmd;
import dev.chrona.plugin.commands.GrantCmd;
import dev.chrona.plugin.commands.PayCmd;
import dev.chrona.plugin.listeners.PlayerJoinListeners;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import dev.chrona.common.Db;

import java.util.Objects;

public final class ChronaPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Db.migrate(this.getClassLoader(), "classpath:db/migration");
        LoggingBootstrap.init(getDataFolder().toPath(), true);

        var logger = ChronaLog.get(ChronaPlugin.class);

        getServer().getPluginManager().registerEvents(new PlayerJoinListeners(), this);
        registerCommand("pay", new PayCmd());
        registerCommand("grant", new GrantCmd());
        registerCommand("balance", new BalanceCmd());
        logger.info("Chrona up.");
    }

    private void registerCommand(String cmd, CommandExecutor executor) {
        Objects.requireNonNull(getCommand(cmd)).setExecutor(executor);
    }
}
