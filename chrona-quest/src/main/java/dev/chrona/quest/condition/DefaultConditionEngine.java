package dev.chrona.quest.condition;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.model.ConditionDef;
import dev.chrona.quest.model.ConditionLogic;
import dev.chrona.quest.model.ConditionType;
import dev.chrona.quest.state.QuestRunState;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Default implementation of the ConditionEngine interface.
 * Evaluates conditions based on the provided ConditionLogic and ConditionContext.
 */
public final class DefaultConditionEngine implements ConditionEngine {

    private static final Logger log = ChronaLog.get(DefaultConditionEngine.class);

    @Override
    public boolean evaluate(ConditionLogic logic, ConditionContext ctx) {
        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(ctx, "ctx");

        // allOf: all must be true
        for (ConditionDef c : logic.allOf()) {
            if (!evalSingle(c, ctx))
                return false;
        }

        // anyOf: if empty -> ignore (true), otherwise at least one true
        if (!logic.anyOf().isEmpty()) {
            boolean any = false;
            for (ConditionDef c : logic.anyOf()) {
                if (evalSingle(c, ctx)) {
                    any = true;
                    break;
                }
            }
            if (!any)
                return false;
        }

        // noneOf: all have to be false
        for (ConditionDef c : logic.noneOf()) {
            if (evalSingle(c, ctx))
                return false;
        }

        return true;
    }

    /**
     * Evaluates a single condition against the provided context.
     *
     * @param cond the condition definition to evaluate
     * @param ctx  the context for evaluation
     * @return true if the condition is met, false otherwise
     */
    private boolean evalSingle(ConditionDef cond, ConditionContext ctx) {
        ConditionType type = cond.type();
        switch (type) {
            case FLAG -> {
                String key = cond.getString("key");
                Boolean equals = cond.getBool("equals");
                if (key == null || equals == null) {
                    log.warn("FLAG condition without key/equals for player {}", ctx.playerId());
                    return false;
                }
                return ctx.flagEquals(key, equals);
            }

            case QUEST_STATE -> {
                String questId = cond.getString("questId");
                String stateStr = cond.getString("state");
                if (questId == null || stateStr == null) {
                    log.warn("QUEST_STATE condition without questId/state for player {}", ctx.playerId());
                    return false;
                }
                QuestRunState expected;
                try {
                    expected = QuestRunState.valueOf(stateStr.toUpperCase());
                }
                catch (IllegalArgumentException ex) {
                    log.warn("Unknown QuestRunState {} in condition for player {}", stateStr, ctx.playerId());
                    return false;
                }
                QuestRunState actual = ctx.questState(questId);
                return actual == expected;
            }

            case JOB_LEVEL -> {
                String job = cond.getString("job");
                Integer minLevel = cond.getInt("minLevel");
                if (job == null || minLevel == null) {
                    log.warn("JOB_LEVEL condition without job/minLevel for player {}", ctx.playerId());
                    return false;
                }
                int lvl = ctx.jobLevel(job);
                return lvl >= minLevel;
            }

            case JOB_ACTIVE -> {
                String job = cond.getString("job");
                if (job == null) {
                    log.warn("JOB_ACTIVE condition without job for player {}", ctx.playerId());
                    return false;
                }
                return ctx.isJobActive(job);
            }

            case HOUSING_LEVEL -> {
                Integer minLevel = cond.getInt("minLevel");
                if (minLevel == null) {
                    log.warn("HOUSING_LEVEL condition without minLevel for player {}", ctx.playerId());
                    return false;
                }
                return ctx.housingLevel() >= minLevel;
            }

            case PLAYER_RANK -> {
                Integer minRankValue = cond.getInt("minRankValue");
                if (minRankValue == null) {
                    log.warn("PLAYER_RANK condition without minRankValue for player {}", ctx.playerId());
                    return false;
                }
                return ctx.rankValue() >= minRankValue;
            }

            case REGION_VISITED -> {
                String regionId = cond.getString("regionId");
                if (regionId == null) {
                    log.warn("REGION_VISITED condition without regionId for player {}", ctx.playerId());
                    return false;
                }
                return ctx.hasVisitedRegion(regionId);
            }

            case REGION_INSIDE -> {
                String regionId = cond.getString("regionId");
                if (regionId == null) {
                    log.warn("REGION_INSIDE condition without regionId for player {}", ctx.playerId());
                    return false;
                }
                return ctx.isInsideRegion(regionId);
            }

            case WORLD_STATE -> {
                String key = cond.getString("key");
                Integer equalsInt = cond.getInt("equals");
                Integer min = cond.getInt("min");
                Integer max = cond.getInt("max");
                if (key == null) {
                    log.warn("WORLD_STATE condition without key for player {}", ctx.playerId());
                    return false;
                }
                int val = ctx.worldStateInt(key, 0);
                if (equalsInt != null && val != equalsInt)
                    return false;
                if (min != null && val < min)
                    return false;
                return max == null || val <= max;
            }

            case EVENT_ACTIVE -> {
                String eventId = cond.getString("eventId");
                if (eventId == null) {
                    log.warn("EVENT_ACTIVE condition without eventId for player {}", ctx.playerId());
                    return false;
                }
                return ctx.isEventActive(eventId);
            }

            case TIME_RANGE -> {
                Integer startHour = cond.getInt("startHour");
                Integer endHour = cond.getInt("endHour");
                if (startHour == null || endHour == null) {
                    log.warn("TIME_RANGE condition without startHour/endHour for player {}", ctx.playerId());
                    return false;
                }
                return ctx.isTimeInRange(startHour, endHour);
            }

            default -> {
                log.warn("Unknown ConditionType {} for player {}", type, ctx.playerId());
                return false;
            }
        }
    }
}
