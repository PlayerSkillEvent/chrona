package dev.chrona.quest.reward;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.action.ActionExecutionContext;
import dev.chrona.quest.action.DefaultActionExecutor;
import dev.chrona.quest.model.ActionDef;
import dev.chrona.quest.model.QuestDefinition;
import dev.chrona.quest.model.RewardDef;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of the RewardEngine interface.
 * Applies rewards to players based on the provided RewardDef.
 */
public final class DefaultRewardEngine implements RewardEngine {

    private static final Logger log = ChronaLog.get(DefaultRewardEngine.class);

    private final DefaultActionExecutor actionExecutor;

    public DefaultRewardEngine(DefaultActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void applyRewards(RewardContext ctx,
                             ActionExecutionContext actionCtx,
                             QuestDefinition quest,
                             RewardDef rewards) {
        if (ctx == null || rewards == null)
            return;

        UUID playerId = ctx.playerId();
        Player player = ctx.player();

        // 1) Currencies
        for (Map.Entry<String, Integer> e : rewards.currencies().entrySet()) {
            String currencyId = e.getKey();
            int amount = e.getValue() != null ? e.getValue() : 0;
            if (amount <= 0)
                continue;

            ctx.addCurrency(playerId, currencyId, amount);
        }

        // 2) Job XP
        for (Map.Entry<String, Integer> e : rewards.jobXp().entrySet()) {
            String key = e.getKey();
            int xp = e.getValue() != null ? e.getValue() : 0;
            if (xp <= 0)
                continue;

            if ("ANY_ACTIVE".equalsIgnoreCase(key))
                ctx.addJobXp(playerId, "ANY_ACTIVE", xp);
            else
                ctx.addJobXp(playerId, key, xp);
        }

        // 3) Housing / Rank XP
        if (rewards.housingXp() > 0)
            ctx.addHousingXp(playerId, rewards.housingXp());
        if (rewards.rankXp() > 0)
            ctx.addRankXp(playerId, rewards.rankXp());

        // 4) Crates
        for (Map<String, Object> crate : rewards.crates()) {
            String crateId = asString(crate.get("crateId"));
            int amount = asInt(crate.get("amount"), 1);
            if (crateId == null || amount <= 0)
                continue;

            ctx.giveCrate(playerId, crateId, amount);
        }

        // 5) Items
        if (player != null) {
            for (Map<String, Object> item : rewards.items()) {
                String itemId = asString(item.get("itemId"));
                int amount = asInt(item.get("amount"), 1);
                boolean bind = asBool(item.get("bindOnPickup"), false);
                if (itemId == null || amount <= 0)
                    continue;

                ctx.giveItem(player, itemId, amount, bind);
            }
        }
        else if (!rewards.items().isEmpty())
            log.warn("Rewards: Player {} is offline, cant give items directly.", playerId);

        // 6) Cosmetics
        for (Map<String, Object> cos : rewards.cosmetics()) {
            String cosmeticId = asString(cos.get("cosmeticId"));
            String variant = asString(cos.getOrDefault("variant", "default"));
            if (cosmeticId == null)
                continue;

            ctx.grantCosmetic(playerId, cosmeticId, variant);
        }

        // 7) Flags
        for (Map.Entry<String, Boolean> e : rewards.flags().entrySet()) {
            String key = e.getKey();
            Boolean value = e.getValue();
            if (key == null || value == null)
                continue;

            ctx.setFlag(playerId, key, value);
        }

        // 8) Extra Actions (z.B. MESSAGE, BROADCAST, WORLD_STATE_SET...)
        List<ActionDef> extraActions = rewards.extraActions();
        if (!extraActions.isEmpty() && actionCtx != null)
            actionExecutor.executeAll(extraActions, actionCtx);

    }

    /** Converts the given object to a string, or null if the object is null. */
    private static String asString(Object o) {
        return o != null ? String.valueOf(o) : null;
    }

    /** Converts the given object to an integer, or returns the default value if conversion fails. */
    private static int asInt(Object o, int def) {
        if (o instanceof Number n)
            return n.intValue();
        if (o == null)
            return def;

        try {
            return Integer.parseInt(String.valueOf(o));
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    /** Converts the given object to a boolean, or returns the default value if conversion fails. */
    private static boolean asBool(Object o, boolean def) {
        if (o instanceof Boolean b)
            return b;
        if (o == null)
            return def;

        return Boolean.parseBoolean(String.valueOf(o));
    }
}
