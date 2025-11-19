package dev.chrona.common.region;

/**
 * 2D point representing a location within a region, defined by x and z coordinates.
 */
public final class RegionPoint {
    private final double x;
    private final double z;

    public RegionPoint(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }
}
