package dev.chrona.plugin;
import dev.chrona.common.log.ChronaLog;
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
        logger.info("Chrona up.");
    }

    private void registerCommand(String cmd, CommandExecutor executor) {
        Objects.requireNonNull(getCommand(cmd)).setExecutor(executor);
    }
}
