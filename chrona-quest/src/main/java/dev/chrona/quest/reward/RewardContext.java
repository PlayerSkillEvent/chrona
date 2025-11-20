package dev.chrona.quest.reward;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Context for rewards
 * Supports rewarding various systems:
 * - Economy / Currencies
 * - JobSystem
 * - HousingSystem
 * - RankSystem
 * - CrateSystem
 * - Item-/Cosmetics-System
 * - FlagSystem
 */
public interface RewardContext {

    /** Returns the player ID associated with this context. */
    UUID playerId();

    /** Returns the player associated with this context, or null if offline or not supported. */
    Player player(); // null, if offline or not supported

    /** Adds currency to the player's account. */
    void addCurrency(UUID playerId, String currencyId, int amount);

    /** Adds job XP to the player's specified job. */
    void addJobXp(UUID playerId, String jobId, int xp);

    /** Adds housing XP to the player's account. */
    void addHousingXp(UUID playerId, int xp);

    /** Adds rank XP to the player's account. */
    void addRankXp(UUID playerId, int xp);

    /** Gives crates to the player. */
    void giveCrate(UUID playerId, String crateId, int amount);

    /** Gives items to the player. */
    void giveItem(Player player, String itemId, int amount, boolean bindOnPickup);

    /** Grants a cosmetic to the player. */
    void grantCosmetic(UUID playerId, String cosmeticId, String variant);

    /** Sets a flag for the player. */
    void setFlag(UUID playerId, String key, boolean value);
}
