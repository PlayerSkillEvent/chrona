package dev.chrona.job.core;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ClasspathSeasonConfigProvider implements JobConfigProvider {
    private final String season;
    private final ClassLoader cl;
    private final Path pluginDataDir;

    public ClasspathSeasonConfigProvider(String season, ClassLoader cl, Path pluginDataDir) {
        this.season = season; this.cl = cl; this.pluginDataDir = pluginDataDir;
    }

    @Override public JobConfig jobConfig(String jobId) {
        String lc = jobId.toLowerCase(Locale.ROOT);
        // 1) plugins/Chrona/seasons/{S}/jobs/{job}.yml
        Path fsPath = pluginDataDir.resolve("seasons").resolve(season)
                .resolve("jobs").resolve(lc + ".yml");
        Map<String,Object> data = null;
        if (Files.exists(fsPath)) {
            try (InputStream in = Files.newInputStream(fsPath)) {
                data = new Yaml().load(in);
            } catch (Exception ignore) {}
        }
        // 2) classpath fallback
        if (data == null) {
            String cp = "seasons/" + season + "/jobs/" + lc + ".yml";
            try (InputStream in = cl.getResourceAsStream(cp)) {
                if (in != null) data = new Yaml().load(in);
            } catch (Exception ignore) {}
        }
        if (data == null) data = Map.of();
        Map<String,Object> root = data;
        return new JobConfig() {
            @Override public long getLong(String path, long def) {
                Object v = dig(path, root);
                if (v instanceof Number n) return n.longValue();
                try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return def; }
            }
            @Override public Map<String, Long> getMapLong(String path) {
                Object v = dig(path, root);
                if (!(v instanceof Map<?,?> m)) return Map.of();
                Map<String, Long> out = new LinkedHashMap<>();
                for (var e : m.entrySet()) {
                    if (e.getKey() == null) continue;
                    long num;
                    Object val = e.getValue();
                    if (val instanceof Number n) num = n.longValue();
                    else {
                        try { num = Long.parseLong(String.valueOf(val)); } catch (Exception ex) { continue; }
                    }
                    out.put(String.valueOf(e.getKey()), num);
                }
                return out;
            }
            @Override public String getString(String path, String def) {
                Object v = dig(path, root);
                return v == null ? def : String.valueOf(v);
            }
            private Object dig(String path, Map<String,Object> node) {
                String[] parts = path.split("\\.");
                Object cur = node;
                for (String p : parts) {
                    if (!(cur instanceof Map<?,?> m)) return null;
                    cur = m.get(p);
                    if (cur == null) return null;
                }
                return cur;
            }
        };
    }
}
