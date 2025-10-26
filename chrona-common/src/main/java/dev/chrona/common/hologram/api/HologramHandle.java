package dev.chrona.common.hologram.api;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface HologramHandle {
    UUID id();
    Location location();

    /** Aktuelle Zeilen setzen (wird allen Viewern gepusht) */
    void setLines(java.util.List<String> lines);
    void setLine(int i, String text);

    /** Viewer-Management (per-Player Sichtbarkeit) */
    void addViewer(Player p);
    void removeViewer(Player p);
    Set<UUID> viewers();
    void setVisible(boolean v);

    /** Position ändern (teleportiert für alle Viewer) */
    void teleport(Location to);

    /** Für alle Viewer zerstören; danach ist das Handle ungültig */
    void destroy();

    Display getEntity();
}