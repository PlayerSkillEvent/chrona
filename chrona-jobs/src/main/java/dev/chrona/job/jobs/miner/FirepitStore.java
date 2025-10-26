package dev.chrona.job.jobs.miner;

import java.util.*;

/** Abstraktion der Firepit-Persistenz (hier: Memory, später: DB/Redis möglich). */
interface FirepitStore {
    Optional<Integer> getLevel(String world, int x, int y, int z);
    void setLevel(String world, int x, int y, int z, int level);
    void reset(String world, int x, int y, int z);
    void clearWorld(String world);
}

final class MemoryFirepitStore implements FirepitStore {
    private final Map<String, Map<BlockKey, Integer>> worlds = new HashMap<>();

    @Override public Optional<Integer> getLevel(String world, int x, int y, int z) {
        var m = worlds.get(world);
        if (m == null) return Optional.empty();
        return Optional.ofNullable(m.get(new BlockKey(x,y,z)));
    }

    @Override public void setLevel(String world, int x, int y, int z, int level) {
        worlds.computeIfAbsent(world, w -> new HashMap<>()).put(new BlockKey(x,y,z), level);
    }

    @Override public void reset(String world, int x, int y, int z) {
        var m = worlds.get(world);
        if (m != null) m.remove(new BlockKey(x,y,z));
    }

    @Override public void clearWorld(String world) { worlds.remove(world); }

    private record BlockKey(int x,int y,int z) {}
}

