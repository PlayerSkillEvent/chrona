package dev.chrona.common.npc.api;

import dev.chrona.common.npc.protocol.PacketNpc;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public interface NpcHandle {
    UUID id();
    UUID uuid();
    String name();
    Location location();

    // Viewer
    void addViewer(Player p);
    void removeViewer(Player p);
    Set<UUID> viewers();

    // Eigenschaften
    void setName(String newName);
    void setSkin(Skin skin);
    void setEquipment(EquipmentSlot slot, ItemStack item); // optional
    void setRotationSource(PacketNpc.RotationSource source);    // null = Blick entlang Pfad/Körper

    // Bewegung
    void teleport(Location to);
    void lookAt(Player viewer, Location target);           // nur für diesen Viewer rotieren

    // Lebenszyklus
    void destroy();

    void runPath(Path path);
    void stopPath();
    void pausePath(boolean pause);
    void resumePath(Path path, int index, int dir, long waitMs);
    NpcPersistence.NpcRuntime state();

    enum EquipmentSlot { MAIN_HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET }
}
