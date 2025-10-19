package dev.chrona.common.economy;

public enum SourceDomain {
    ECONOMY,   // generic econ ops (transfers, fees)
    JOB,       // jobs (miner/farmer/…)
    QUEST,     // quests
    MARKET,    // bay/market buy/sell
    HOUSING,   // plots, taxes, rent
    SYSTEM;    // admin, migration, debug

    @Override public String toString() { return name(); }
}
