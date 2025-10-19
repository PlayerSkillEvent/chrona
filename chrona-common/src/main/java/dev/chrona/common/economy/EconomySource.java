package dev.chrona.common.economy;
public enum EconomySource implements ChronaSource {
    PLAYER_TRADE, FEE, INTEREST;

    @Override public SourceDomain domain() { return SourceDomain.ECONOMY; }
    @Override public String key() { return name().toLowerCase(); }
}
