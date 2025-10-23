package dev.chrona.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public final class ChronaLog {
    private ChronaLog() {}

    public static Logger get(Class<?> type) {
        return LoggerFactory.getLogger(type);
    }

    public static void error(Logger log, Throwable ex, String message, Object... args) {
        if (ex == null) {
            log.error(message, args);
            return;
        }

        log.error("{} - {}: {}", message, ex.getClass().getSimpleName(), ex.getMessage(), ex);
    }

    public static void error(Logger log, Marker marker, Throwable ex, String message, Object... args) {
        if (ex == null) {
            log.error(marker, message, args);
            return;
        }

        log.error(marker, "{} - {}: {}", message, ex.getClass().getSimpleName(), ex.getMessage(), ex);
    }
}
