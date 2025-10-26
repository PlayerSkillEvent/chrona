package dev.chrona.job.jobs.miner;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.job.api.Job;
import dev.chrona.job.core.JobContext;
import dev.chrona.job.core.JobPlayerState;
import dev.chrona.job.core.JobRuntime;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

import org.slf4j.Logger;

/**
 * CHRONA – JOB: MINER (MIN) – Version 2.0
 * - Feuerstelle erhitzen (Minigame) -> Glut-Level (1..3)
 * - Wasserschlag anwenden -> Loot nach Level, Reset
 * - Hologramm zeigt Glut-Level per Spieler (packetbasiert, kein Server-Entity)
 * - Keine DB: Firepits leben im MemoryStore pro Welt/Instanz
 */
public final class MinerJob implements Job {
    private static final Set<Material> FIREPIT_BLOCKS = Set.of(Material.NETHERRACK, Material.MAGMA_BLOCK, Material.DEEPSLATE);
    private static final Set<String>   ALLOWED_WORLDS = Set.of("hoehleregion_1","hoehleregion_2","hoehleregion_3"); // Beispiel-Instanzen
    private static final long HEAT_COOLDOWN_MS   = 1200; // 1.2s
    private static final long QUENCH_COOLDOWN_MS = 1200;

    private Logger logger;
    private JobContext ctx;
    private FirepitManager firepits;

    private final Map<UUID, Long> heatCd   = new HashMap<>();
    private final Map<UUID, Long> quenchCd = new HashMap<>();

    @Override
    public String id() {
        return "MINER";
    }
    @Override
    public String displayName() {
        return "Miner";
    }

    @Override
    public void onEnable(JobContext ctx) {
        this.ctx = ctx;
        this.logger = ChronaLog.get(MinerJob.class);
        // In-Memory Store (pro Instanz/Welt)
        this.firepits = new FirepitManager(ctx.holo(), new MemoryFirepitStore());
        logger.info("[Miner] Enabled – in-memory Firepits, Holograms via ProtocolLib");
    }

    @Override public void onDisable() {
        // Instanz-Reset extern aufrufen: firepits.clearWorld(world)
    }

    @Override
    public Collection<Listener> listeners(JobRuntime runtime) {
        return List.of(new MinerListener(runtime));
    }

    final class MinerListener implements Listener {
        private final JobRuntime runtime;

        MinerListener(JobRuntime runtime) {
            this.runtime = runtime;
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
        public void onInteract(PlayerInteractEvent e) {
            if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
                return;
            Block block = e.getClickedBlock();

            if (block == null)
                return;
            if (!FIREPIT_BLOCKS.contains(block.getType()))
                return;

            World w = block.getWorld();
            if (!ALLOWED_WORLDS.isEmpty() && !ALLOWED_WORLDS.contains(w.getName()))
                return;

            Player p = e.getPlayer();
            e.setCancelled(true);

            if (MinerItems.isWaterStrike(p.getInventory().getItemInMainHand())
                    || MinerItems.isWaterStrike(p.getInventory().getItemInOffHand())) {
                handleQuench(p, block);
                return;
            }

            handleHeat(p, block, runtime);
        }

        private void handleHeat(Player p, Block block, JobRuntime runtime) {
            if (checkCooldown(heatCd, p.getUniqueId(), HEAT_COOLDOWN_MS))
                return;

            var fp = firepits.getOrInit(block.getLocation());

            if (fp.level() >= 3) {
                p.playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
                p.sendMessage("§7Diese Glutstelle ist bereits auf §cStufe 3§7.");
                fp.ensureHologramFor(p);
                return;
            }

            ctx.minigames().start("PRESS_QTE", p, Map.of("steps", 3, "perStepMs", 1800))
                    .thenAccept(res -> {
                        if (!p.isOnline())
                            return;

                        if (!res.success()) {
                            p.sendMessage("§7Heizen fehlgeschlagen.");
                            p.playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 0.9f);
                            return;
                        }

                        Runnable r = () -> {
                            fp.increase();
                            fp.ensureHologramFor(p);
                            p.sendMessage("§6Glut erhitzt: §eStufe " + fp.level() + "§7/§e3");
                            p.playSound(block.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, 1.0f);
                            block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5,1.0,0.5), 8, 0.2,0.2,0.2, 0.01);
                        };

                        if (Bukkit.isPrimaryThread())
                            r.run();
                        else
                            Bukkit.getScheduler().runTask(ctx.plugin(), r);
                    });
        }

        private void handleQuench(Player p, Block block) {
            if (checkCooldown(quenchCd, p.getUniqueId(), QUENCH_COOLDOWN_MS))
                return;

            var opt = firepits.find(block.getLocation());
            if (opt.isEmpty() || opt.get().level() == 0) {
                p.sendMessage("§7Diese Glutstelle hat keine Hitze gespeichert.");
                p.playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 0.8f);
                return;
            }

            var fp = opt.get();
            int level = fp.level();

            int playerLevel = runtime.getState(p.getUniqueId(), id()).map(JobPlayerState::level).orElse(1);
            double bonusL3 = (playerLevel >= 10) ? 0.10 : 0.0;

            var drops = MinerLoot.roll(level, bonusL3);

            fp.reset();
            fp.ensureHologramFor(p);

            MinerLoot.give(p, drops);

            var loc = block.getLocation();
            var w = loc.getWorld();
            w.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f);
            w.spawnParticle(Particle.CLOUD, loc.add(0.5,1.0,0.5), 15, 0.25,0.2,0.25, 0.01);
            p.sendMessage(MinerText.summarize(level, drops));
        }
    }

    private static boolean checkCooldown(Map<UUID, Long> map, UUID id, long windowMs) {
        long now = System.currentTimeMillis();
        long last = map.getOrDefault(id, 0L);
        if (now - last < windowMs)
            return true;
        map.put(id, now);
        return false;
    }
}
