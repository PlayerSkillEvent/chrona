package dev.chrona.plugin;
import org.bukkit.plugin.java.JavaPlugin;
import dev.chrona.common.Db;

public final class ChronaPlugin extends JavaPlugin {

    @Override public void onEnable() {
        Db.migrate(this.getClassLoader(), "classpath:db/migration");
        getServer().getPluginManager().registerEvents(new PlayerJoinListeners(), this);
        getCommand("pay").setExecutor(new PayCmd());
        getCommand("grant").setExecutor(new GrantCmd());
        getCommand("balance").setExecutor(new BalanceCmd());
        getLogger().info("Chrona up.");
    }
}
