package dev.chrona.job.jobs.miner;

import org.bukkit.Location;

import java.util.Objects;

record FirepitKey(String world, int x, int y, int z) {
    static FirepitKey of(Location l) {
        return new FirepitKey(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FirepitKey(String world1, int x1, int y1, int z1)))
            return false;

        return x == x1 && y == y1 && z == z1 && Objects.equals(world, world1);
    }
}

