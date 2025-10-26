package dev.chrona.job.jobs.miner;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;

public final class MinerItems {

    public static ItemStack waterStrike() {
        var it = new ItemStack(Material.PRISMARINE_SHARD);
        var im = it.getItemMeta();
        im.displayName(Component.text("§bWasserschlag"));
        im.lore(List.of(Component.text("§7Nutze dies auf einer Glutstelle,"), Component.text("§7um abzuschrecken und Loot zu erhalten.")));
        it.setItemMeta(im);
        return it;
    }

    public static boolean isWaterStrike(ItemStack it) {
        if (it == null || it.getType() != Material.PRISMARINE_SHARD) return false;
        ItemMeta im = it.getItemMeta();
        if (im == null || !im.hasDisplayName()) return false;
        return Objects.equals(((TextComponent)im.displayName()).content(), "§bWasserschlag");
    }
}

