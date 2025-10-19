package dev.chrona.common.economy;
public enum SystemSource implements ChronaSource {
    ADMIN_GRANT, MIGRATION, DEBUG;

    @Override public SourceDomain domain() { return SourceDomain.SYSTEM; }
    @Override public String key() { return name().toLowerCase(); }
}
