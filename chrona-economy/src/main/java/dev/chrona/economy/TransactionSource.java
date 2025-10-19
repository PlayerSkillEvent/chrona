package dev.chrona.economy;

public enum TransactionSource {
    /** Manual Admin-Gran (/grant) */
    ADMIN_GRANT,

    /** Player-to-Player-Transfer (/pay) */
    PLAYER_TRADE,

    /** Quest-Reward */
    QUEST_REWARD,

    /** Job-Reward (e.g. Miner, Farmer) */
    JOB_REWARD,

    /** Market-PURCHASE/SALE */
    MARKET_PURCHASE,
    MARKET_SALE,

    /** Housing-Cost, Plot taxes etc. */
    HOUSING_RENT,

    /** Others (e.g. Debug or Migration) */
    OTHER;

    @Override public String toString() { return name().toLowerCase(); }
}
