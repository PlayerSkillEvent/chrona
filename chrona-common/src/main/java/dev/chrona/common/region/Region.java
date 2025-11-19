package dev.chrona.common.region;

import org.bukkit.Location;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A region in a world defined by a 2D polygon and vertical bounds.
 * <p>
 * - 2D polygon on the XZ-plane
 * - minY/maxY for vertical bounds
 *
 * @param priority for overbound later (higher = more important)
 */
public record Region(String id, String displayName, String worldName, RegionType type, List<RegionPoint> polygon,
                     int minY, int maxY, int priority) {

    public Region(String id, String displayName, String worldName, RegionType type, List<RegionPoint> polygon, int minY,
            int maxY, int priority)
    {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName != null ? displayName : id;
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.type = Objects.requireNonNull(type, "type");
        if (polygon == null || polygon.size() < 3)
            throw new IllegalArgumentException("Region " + id + " must have at least 3 polygon points");

        this.polygon = List.copyOf(polygon);
        this.minY = minY;
        this.maxY = maxY;
        this.priority = priority;
    }

    @Override
    public List<RegionPoint> polygon() {
        return Collections.unmodifiableList(polygon);
    }

    /**
     * Checks if the given location is inside this region.
     */
    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;

        int y = loc.getBlockY();
        if (y < minY || y > maxY) return false;

        double x = loc.getX();
        double z = loc.getZ();
        return pointInPolygon(x, z);
    }

    /**
     * Ray-casting algorithm to determine if point is in polygon.
     */
    private boolean pointInPolygon(double x, double z) {
        boolean inside = false;
        List<RegionPoint> pts = polygon;
        for (int i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
            double xi = pts.get(i).x();
            double zi = pts.get(i).z();
            double xj = pts.get(j).x();
            double zj = pts.get(j).z();

            boolean intersects = ((zi > z) != (zj > z)) &&
                    (x < (xj - xi) * (z - zi) / (zj - zi + 0.0) + xi);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    @Override
    public String toString() {
        return "Region{" +
                "id='" + id + '\'' +
                ", world='" + worldName + '\'' +
                ", minY=" + minY +
                ", maxY=" + maxY +
                ", type=" + type +
                ", priority=" + priority +
                '}';
    }
}
