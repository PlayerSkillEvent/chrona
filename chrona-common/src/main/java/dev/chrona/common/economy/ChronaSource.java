package dev.chrona.common.economy;

public interface ChronaSource {
    SourceDomain domain();
    String key(); // short stable identifier, e.g. "player_trade", "admin_grant"

    default String fullKey() { return domain().name().toLowerCase() + ":" + key(); }
}
