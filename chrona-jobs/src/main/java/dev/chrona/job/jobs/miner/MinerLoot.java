package dev.chrona.job.jobs.miner;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loot:
 * - Level 1: Gesteinsfragmente + bisschen Kupfer
 * - Level 2: mehr Kupfer, Chance auf Türkis
 * - Level 3: sehr gutes Erz, Goldchance, Fossilien
 * Bonus: +chanceL3 aus Perk (z.B. +10% bei Job-Level >= 10)
 */
final class MinerLoot {

    static List<ItemStack> roll(int level, double bonusChanceL3) {
        var r = ThreadLocalRandom.current();
        var drops = new ArrayList<ItemStack>();

        drops.add(new ItemStack(Material.FLINT, 1 + r.nextInt(level + 1)));

        if (level >= 1) {
            if (r.nextDouble() < (0.6 + 0.1 * Math.min(level, 2)))
                drops.add(new ItemStack(Material.COPPER_ORE, 1 + r.nextInt(2 + level)));

        }
        if (level >= 2) {
            double chanceTurkis = 0.15 + 0.05 * (level - 1);
            if (r.nextDouble() < chanceTurkis)
                drops.add(new ItemStack(Material.PRISMARINE_CRYSTALS, 1));

        }
        if (level >= 3) {
            double goldChance = 0.10 + bonusChanceL3; // Perk
            if (r.nextDouble() < goldChance)
                drops.add(new ItemStack(Material.RAW_GOLD, 1));

            double fossilChance = 0.07 + (bonusChanceL3 * 0.5);
            if (r.nextDouble() < fossilChance)
                drops.add(new ItemStack(Material.BONE_BLOCK, 1));
        }
        return drops;
    }

    static void give(Player p, List<ItemStack> items) {
        for (var it : items) {
            var left = p.getInventory().addItem(it);
            if (!left.isEmpty())
                left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
        }
    }
}
