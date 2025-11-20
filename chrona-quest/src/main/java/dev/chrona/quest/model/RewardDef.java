package dev.chrona.quest.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the rewards given upon quest completion.
 */
public final class RewardDef {

    // currencies
    private final Map<String, Integer> currencies; // z.B. "deben" -> 150, "eventTokens" -> 5

    // xp
    private final Map<String, Integer> jobXp;      // "MINER" -> 300, "ANY_ACTIVE" -> 50
    private final int housingXp;
    private final int rankXp;

    // crates/items/cosmetics
    private final List<Map<String, Object>> crates;
    private final List<Map<String, Object>> items;
    private final List<Map<String, Object>> cosmetics;

    // flags
    private final Map<String, Boolean> flags;

    // extra actions
    private final List<ActionDef> extraActions;

    public RewardDef(
            Map<String, Integer> currencies,
            Map<String, Integer> jobXp,
            int housingXp,
            int rankXp,
            List<Map<String, Object>> crates,
            List<Map<String, Object>> items,
            List<Map<String, Object>> cosmetics,
            Map<String, Boolean> flags,
            List<ActionDef> extraActions
    ) {
        this.currencies = currencies != null ? Map.copyOf(currencies) : Map.of();
        this.jobXp = jobXp != null ? Map.copyOf(jobXp) : Map.of();
        this.housingXp = housingXp;
        this.rankXp = rankXp;
        this.crates = crates != null ? List.copyOf(crates) : List.of();
        this.items = items != null ? List.copyOf(items) : List.of();
        this.cosmetics = cosmetics != null ? List.copyOf(cosmetics) : List.of();
        this.flags = flags != null ? Map.copyOf(flags) : Map.of();
        this.extraActions = extraActions != null ? List.copyOf(extraActions) : List.of();
    }

    /**
     * Returns an unmodifiable map of currency rewards.
     *
     * @return the currency rewards map
     */
    public Map<String, Integer> currencies() {
        return currencies;
    }

    /**
     * Returns an unmodifiable map of job XP rewards.
     *
     * @return the job XP rewards map
     */
    public Map<String, Integer> jobXp() {
        return jobXp;
    }

    /**
     * Returns the housing XP reward.
     *
     * @return the housing XP
     */
    public int housingXp() {
        return housingXp;
    }

    /**
     * Returns the rank XP reward.
     *
     * @return the rank XP
     */
    public int rankXp() {
        return rankXp;
    }

    /**
     * Returns an unmodifiable list of crate rewards.
     *
     * @return the list of crate rewards
     */
    public List<Map<String, Object>> crates() {
        return crates;
    }

    /**
     * Returns an unmodifiable list of item rewards.
     *
     * @return the list of item rewards
     */
    public List<Map<String, Object>> items() {
        return items;
    }

    /**
     * Returns an unmodifiable list of cosmetic rewards.
     *
     * @return the list of cosmetic rewards
     */
    public List<Map<String, Object>> cosmetics() {
        return cosmetics;
    }

    /**
     * Returns an unmodifiable map of flag rewards.
     *
     * @return the flag rewards map
     */
    public Map<String, Boolean> flags() {
        return flags;
    }

    /**
     * Returns an unmodifiable list of extra action rewards.
     *
     * @return the list of extra action rewards
     */
    public List<ActionDef> extraActions() {
        return extraActions;
    }
}
