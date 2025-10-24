package dev.chrona.minigames.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MinigameRouterListener implements Listener {
    private final MinigameManager mgr;
    public MinigameRouterListener(MinigameManager mgr) { this.mgr = mgr; }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        mgr.cancelActive(e.getPlayer().getUniqueId(), "quit");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // Wird von TimingBarGame benutzt (Klick = "STOP")
        // Keine Logik hier; das Game selbst subscribed separat, wenn es läuft.
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // PRESS_QTE nutzt sein eigenes Inventory; dort geprüft
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Games können Close als Abbruch werten – handhaben sie selbst
    }
}

