package dev.chrona.common.log;
import org.slf4j.MDC;
import java.util.UUID;

public final class MDCs implements AutoCloseable {

    public static MDCs player(UUID id, String name, String world, String corr) {
        MDC.put("player.id", id != null ? id.toString() : "-");
        MDC.put("player.name", name != null ? name : "-");
        MDC.put("world", world != null ? world : "-");
        MDC.put("corr", corr != null ? corr : "-");
        return new MDCs();
    }

    @Override public void close() { MDC.clear(); }
}
