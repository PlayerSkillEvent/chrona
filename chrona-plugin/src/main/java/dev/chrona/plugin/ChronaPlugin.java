package dev.chrona.plugin;

import dev.chrona.common.dialogue.DialogueService;
import dev.chrona.common.hologram.protocol.ProtocolHolograms;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.common.log.LoggingBootstrap;
import dev.chrona.common.npc.api.NpcPersistence;
import dev.chrona.common.npc.api.SkinService;
import dev.chrona.common.npc.protocol.NpcController;
import dev.chrona.common.npc.protocol.ProtocolNpcs;
import dev.chrona.common.region.*;
import dev.chrona.economy.PgEconomy;
import dev.chrona.economy.PlayerRepo;
import dev.chrona.job.core.*;
import dev.chrona.minigames.core.MinigameManager;
import dev.chrona.plugin.commands.*;
import dev.chrona.plugin.listeners.DialogueListener;
import dev.chrona.plugin.listeners.JoinListener;
import dev.chrona.minigames.Minigames;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import dev.chrona.common.Db;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;
import java.util.Objects;

public final class ChronaPlugin extends JavaPlugin {

    private ProtocolHolograms holoService;
    private PgEconomy econ;
    private MinigameManager minigames;
    private ProtocolNpcs npcs;
    private PlayerRepo playerRepo;
    private NpcController npcCtrl;
    private NpcPersistence persistence;
    private DialogueService dialogueService;
    private RegionService regionService;
    private RegionVisitLogService regionVisitLogService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Db.migrate(this.getClassLoader(), "classpath:db/migration");
        LoggingBootstrap.init(getDataFolder().toPath(), true);
        DataSource ds = getDs();
        var logger = ChronaLog.get(ChronaPlugin.class);

        holoService = new ProtocolHolograms();
        econ = new PgEconomy(ds);
        minigames = Minigames.init(this);
        npcCtrl = new NpcController();
        npcs = new ProtocolNpcs(this, npcCtrl);
        playerRepo = new PlayerRepo(ds);

        SkinService skins = new SkinService();
        NpcPersistence.NpcFactory factory = (loc, name, skin) -> getNpcs().create(loc, name, skin);
        NpcCommand npcCmd = new NpcCommand(this, factory, npcCtrl, skins);
        this.persistence = new NpcPersistence(this, npcCtrl, factory);

        List<NpcPersistence.NpcRuntime> runtimes = persistence.loadAllAndRecreate();
        logger.info("Loaded {} NPCs from storage.", runtimes.size());

        Bukkit.getScheduler().runTask(this, () -> {
            for (var name : npcCtrl.listNames()) {
                var npc = npcCtrl.get(name);
                for (Player player : Bukkit.getOnlinePlayers())
                    npc.addViewer(player);
            }
        });

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            persistence.saveAll(npcCtrl.runtimes());
        }, 1200L, 1200L);

        this.dialogueService = new DialogueService(this);

        initRegions();
        initRegionLogging();

        registerCommand("wallet", new WalletCmd(econ));
        registerCommand("pay", new PayCmd(econ));
        registerCommand("econ", new EconCmd(econ));
        registerCommand("minergive", new MinerGiveCmd());
        registerCommand("npcpath", new NpcPathCommand(this));
        registerCommand("npc", npcCmd);
        registerCommand("dialogue", new DialogueCmd(dialogueService));

        registerEvent(new JoinListener(this, playerRepo));
        registerEvent(npcs);
        registerEvent(new DialogueListener(dialogueService));

        Objects.requireNonNull(getCommand("npc")).setTabCompleter(npcCmd);

        String season = getConfig().getString("season", "S1");
        JobConfigProvider cfgProvider = new ClasspathSeasonConfigProvider(season, getClassLoader(), getDataFolder().toPath());
        JobContext ctx = new JobContext(this, ds, econ, minigames, holoService, cfgProvider);
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
        persistence.saveAll(npcCtrl.runtimes());
        for (var name : npcCtrl.listNames()) {
            var npc = npcCtrl.get(name);
            npc.destroy();
        }
    }

    private void registerCommand(String cmd, CommandExecutor executor) {
        Objects.requireNonNull(getCommand(cmd)).setExecutor(executor);
    }

    public ProtocolHolograms getHoloService() {
        return holoService;
    }

    public MinigameManager getMinigames() {
        return minigames;
    }

    public PgEconomy getEcon() {
        return econ;
    }

    public PlayerRepo getPlayerRepo() {
        return playerRepo;
    }

    public ProtocolNpcs getNpcs() {
        return npcs;
    }

    public DialogueService getDialogueService() { return dialogueService; }

    public RegionService getRegionService() {
        return regionService;
    }

    public RegionVisitLogService getRegionVisitLogService() {
        return regionVisitLogService;
    }

    private void initRegions() {
        File regionsFile = new File(getDataFolder(), "regions.yml");
        if (!regionsFile.exists())
            saveResource("regions.yml", false); // optional default

        List<Region> regions = RegionConfigLoader.loadFromFile(regionsFile);

        regionService = new RegionService(this);
        regionService.setRegions(regions);
        getServer().getPluginManager().registerEvents(regionService, this);

        RegionApi.init(regionService);
    }

    private void initRegionLogging() {
        regionVisitLogService = new RegionVisitLogService(this);
        RegionVisitLogListener listener = new RegionVisitLogListener(regionVisitLogService);
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
