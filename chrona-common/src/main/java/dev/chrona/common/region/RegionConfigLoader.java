package dev.chrona.common.region;

import dev.chrona.common.log.ChronaLog;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class RegionConfigLoader {

    private static final Logger log = ChronaLog.get(RegionConfigLoader.class);

    private RegionConfigLoader() {}

    /** Loads regions from the given YAML file. */
    public static List<Region> loadFromFile(File file) {
        List<Region> regions = new ArrayList<>();

        if (!file.exists()) {
            log.warn("regions.yml not found under {} – no regions loaded.", file.getAbsolutePath());
            return regions;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (Object obj : cfg.getMapList("regions")) {
            if (!(obj instanceof java.util.Map<?, ?> map))
                continue;

            String id = asString(map.get("id"));
            if (id == null || id.isBlank()) {
                log.warn("Region with unknown id {} – ignored.", file.getName());
                continue;
            }

            String name = asString(map.get("name"));
            String world = asString(map.get("world"));
            String typeStr = asString(map.get("type"));
            if (world == null || typeStr == null) {
                log.warn("Region {} has no world/type – ignored.", id);
                continue;
            }

            RegionType type;
            try {
                type = RegionType.valueOf(typeStr.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                log.warn("Region {} has unknown type {} – ignored.", id, typeStr);
                continue;
            }

            int minY = asInt(map.get("minY"), Integer.MIN_VALUE);
            int maxY = asInt(map.get("maxY"), Integer.MAX_VALUE);
            int priority = asInt(map.get("priority"), 0);

            List<RegionPoint> points = switch (type) {
                case RECT -> parseRect(map, id);
                case POLYGON -> parsePolygon(map, id);
            };
            if (points == null || points.size() < 3) {
                log.warn("Region {} has too few Punkte – ignored.", id);
                continue;
            }

            Region region = new Region(id, name, world, type, points, minY, maxY, priority);
            regions.add(region);
        }

        log.info("RegionConfigLoader: {} regions loaded from {}.", regions.size(), file.getName());
        return regions;
    }

    @SuppressWarnings("unchecked")
    private static List<RegionPoint> parsePolygon(java.util.Map<?, ?> map, String id) {
        Object polyObj = map.get("polygon");
        if (!(polyObj instanceof List<?> list)) {
            log.warn("Region {} type=POLYGON has no 'polygon'-array.", id);
            return null;
        }

        List<RegionPoint> points = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof java.util.Map<?, ?> p))
                continue;
            Double x = asDouble(p.get("x"));
            Double z = asDouble(p.get("z"));
            if (x == null || z == null) {
                log.warn("Region {} polygon has invalid point: {}", id, p);
                continue;
            }
            points.add(new RegionPoint(x, z));
        }
        return points;
    }

    /** Parses rectangle region points from the given map. */
    private static List<RegionPoint> parseRect(java.util.Map<?, ?> map, String id) {
        Object rectObj = map.get("rect");
        if (!(rectObj instanceof java.util.Map<?, ?> rect)) {
            log.warn("Region {} type=RECT has no 'rect'-object.", id);
            return null;
        }

        Double x1 = asDouble(rect.get("x1"));
        Double z1 = asDouble(rect.get("z1"));
        Double x2 = asDouble(rect.get("x2"));
        Double z2 = asDouble(rect.get("z2"));

        if (x1 == null || z1 == null || x2 == null || z2 == null) {
            log.warn("Region {} RECT has uncomplete rect-coordinates.", id);
            return null;
        }

        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minZ = Math.min(z1, z2);
        double maxZ = Math.max(z1, z2);

        List<RegionPoint> points = new ArrayList<>();
        points.add(new RegionPoint(minX, minZ));
        points.add(new RegionPoint(maxX, minZ));
        points.add(new RegionPoint(maxX, maxZ));
        points.add(new RegionPoint(minX, maxZ));
        return points;
    }

    /** Converts the given object to a string, or null if the object is null. */
    private static String asString(Object o) {
        return o != null ? String.valueOf(o) : null;
    }

    /** Converts the given object to an integer, or returns the default value if conversion fails. */
    private static Integer asInt(Object o, int def) {
        if (o == null)
            return def;
        if (o instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    /** Converts the given object to a double, or null if conversion fails. */
    private static Double asDouble(Object o) {
        if (o == null)
            return null;
        if (o instanceof Number n)
            return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
