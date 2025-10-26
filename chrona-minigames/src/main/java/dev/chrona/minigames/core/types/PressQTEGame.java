package dev.chrona.minigames.core.types;

import dev.chrona.minigames.api.Minigame;
import dev.chrona.minigames.api.MinigameResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PressQTEGame implements Minigame, Listener {

    private final Plugin plugin;

    public PressQTEGame(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "PRESS_QTE"; }

    @Override
    public CompletableFuture<MinigameResult> start(Player p, Map<String, Object> data) {
        var result = new CompletableFuture<MinigameResult>();

        int steps = getI(data, "steps", 3);
        int perStepMs = getI(data, "perStepMs", 2000);
        var inv = Bukkit.createInventory(null, 9, Component.text("§6Press QTE"));
        var sequence = randomSequence(steps);
        var startMs = System.currentTimeMillis();

        // draw hints
        for (int i = 0; i < sequence.size(); i++) {
            int slot = sequence.get(i);
            inv.setItem(slot, numberedPane(i+1));
        }

        p.openInventory(inv);

        class State {
            int index = 0;
            boolean done = false;
            int ticks = 0;
            long lastStepAt = System.currentTimeMillis();
            int totalTimeMs = 0;
        }
        var st = new State();

        Listener local = new Listener() {
            @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
            public void onClick(InventoryClickEvent e) {
                if (st.done)
                    return;

                if (!Objects.equals(e.getClickedInventory(), inv))
                    return;

                if (!(e.getWhoClicked().getUniqueId().equals(p.getUniqueId())))
                    return;

                e.setCancelled(true);

                int slot = e.getSlot();
                if (slot == sequence.get(st.index)) {
                    st.totalTimeMs += (int)(System.currentTimeMillis() - st.lastStepAt);
                    st.lastStepAt = System.currentTimeMillis();
                    inv.setItem(slot, successPane(st.index+1));
                    st.index++;
                    if (st.index >= sequence.size()) {
                        complete(true);
                    }
                } else {
                    complete(false);
                }
            }

            @EventHandler
            public void onClose(InventoryCloseEvent e) {
                if (st.done) return;
                if (!Objects.equals(e.getInventory(), inv)) return;
                if (e.getPlayer().getUniqueId().equals(p.getUniqueId())) complete(false);
            }
        };
        Bukkit.getPluginManager().registerEvents(local, plugin);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (st.done) return;
            // timeout pro step
            if (System.currentTimeMillis() - st.lastStepAt > perStepMs) {
                complete(false);
            }
        }, 1L, 1L);

        Runnable cleanup = () -> {
            st.done = true;
            try { p.closeInventory(); } catch (Exception ignore) {}
            HandlerList.unregisterAll(local);
            Bukkit.getScheduler().cancelTask(taskId);
        };

        Runnable finish = (/*none*/) -> { };

        Consumer<Boolean> complete = (Boolean success) -> {
            if (st.done) return;
            cleanup.run();
            long elapsed = System.currentTimeMillis() - startMs;
            if (!success) {
                result.complete(MinigameResult.fail(elapsed));
                return;
            }
            double acc = 1.0; // alle Schritte korrekt
            int base = 100;
            int timeBonus = Math.max(0, (steps * perStepMs - st.totalTimeMs) / 50); // 1 Punkt pro 50ms „gespart“
            int score = Math.min(100, base - (st.totalTimeMs / 30) + timeBonus);
            score = Math.max(10, score);
            result.complete(MinigameResult.success(score, acc, elapsed));
        };

        // store refs (for lambda captures)
        this._cleanup = cleanup;
        this._completeQte = complete;

        return result;
    }

    private Runnable _cleanup = () -> {};
    private java.util.function.Consumer<Boolean> _completeQte = b -> {};
    private void complete(boolean success) { _completeQte.accept(success); }

    private static ItemStack numberedPane(int n) {
        var it = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        var im = it.getItemMeta(); im.displayName(Component.text("§eStep " + n)); it.setItemMeta(im);
        return it;
    }
    private static ItemStack successPane(int n) {
        var it = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var im = it.getItemMeta(); im.displayName(Component.text("§aOK " + n)); it.setItemMeta(im);
        return it;
    }
    private static List<Integer> randomSequence(int steps) {
        var slots = new ArrayList<>(List.of(1,3,5,7));
        java.util.Collections.shuffle(slots);
        return slots.subList(0, Math.min(steps, slots.size()));
    }

    private static int getI(Map<String,Object> m, String k, int def) {
        if (m == null) return def;
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}

