package dev.chrona.job.jobs.miner;

import dev.chrona.common.hologram.protocol.ProtocolHolograms;
import org.bukkit.Location;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class FirepitManager {
    private final ProtocolHolograms holos;
    private final FirepitStore store;
    private final Map<FirepitKey, Firepit> cache = new ConcurrentHashMap<>();

    FirepitManager(ProtocolHolograms holos, FirepitStore store) {
        this.holos = holos;
        this.store = store;
    }

    Firepit getOrInit(Location loc) {
        var key = FirepitKey.of(loc);
        return cache.computeIfAbsent(key, k -> {
            int level = store.getLevel(k.world(), k.x(), k.y(), k.z()).orElse(0);
            return new Firepit(holos, k, level);
        });
    }

    Optional<Firepit> find(Location loc) {
        var key = FirepitKey.of(loc);
        var cached = cache.get(key);
        if (cached != null) return Optional.of(cached);
        var lvl = store.getLevel(key.world(), key.x(), key.y(), key.z());
        return lvl.map(v -> {
            var fp = new Firepit(holos, key, v);
            cache.put(key, fp);
            return fp;
        });
    }

    void clearWorld(String worldName) {
        cache.keySet().removeIf(k -> {
            if (k.world().equals(worldName)) {
                var fp = cache.get(k);
                if (fp != null)
                    fp.destroyAllHolograms();
                return true;
            }
            return false;
        });
        store.clearWorld(worldName);
    }
}
