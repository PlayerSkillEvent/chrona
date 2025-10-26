package dev.chrona.job.jobs.miner;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

final class MinerText {
    static String summarize(int level, List<ItemStack> drops) {
        StringBuilder sb = new StringBuilder();
        sb.append("§bAbschrecken §7(Glut ").append(level).append(") → ");
        for (int i = 0; i < drops.size(); i++) {
            var it = drops.get(i);
            sb.append("§f").append(it.getAmount()).append("× ").append(pretty(it.getType()));
            if (i < drops.size() - 1)
                sb.append("§7, ");
        }
        return sb.toString();
    }

    static String pretty(Material m) {
        return switch (m) {
            case COPPER_ORE    -> "Kupfererz";
            case RAW_GOLD      -> "Rohgold";
            case PRISMARINE_CRYSTALS -> "Türkis";
            case BONE_BLOCK    -> "Fossil";
            case FLINT         -> "Gesteinsfrag.";
            default -> m.name();
        };
    }
}
