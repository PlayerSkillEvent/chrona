package dev.chrona.common;

import java.nio.file.*;
import java.util.*;

public final class Env {

    private static final Properties P = new Properties();

    static {
        var f = Path.of(".env.local");
        if (Files.exists(f)) {
            try (var s = Files.lines(f)) {
                s.filter(l -> !l.isBlank() && !l.startsWith("#")).forEach(l -> {
                    int i = l.indexOf('=');
                    if (i > 0) P.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
                });
            }
            catch (Exception ignore) {}
        }
    }

    public static String get(String k, String def) {
        return System.getenv().getOrDefault(k, P.getProperty(k, def));
    }
}
