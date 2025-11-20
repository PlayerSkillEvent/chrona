package dev.chrona.quest.engine;

import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.action.ActionContextFactory;
import dev.chrona.quest.action.ActionExecutionContext;
import dev.chrona.quest.action.DefaultActionExecutor;
import dev.chrona.quest.condition.ConditionContext;
import dev.chrona.quest.condition.ConditionContextFactory;
import dev.chrona.quest.condition.ConditionEngine;
import dev.chrona.quest.config.QuestRegistry;
import dev.chrona.quest.model.ObjectiveDef;
import dev.chrona.quest.model.QuestDefinition;
import dev.chrona.quest.model.QuestFlowMode;
import dev.chrona.quest.model.QuestRepeatability;
import dev.chrona.quest.model.RewardDef;
import dev.chrona.quest.state.*;
import dev.chrona.quest.reward.RewardContext;
import dev.chrona.quest.reward.RewardContextFactory;
import dev.chrona.quest.reward.RewardEngine;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Default implementation of the QuestEngine interface.
 * Manages quest states, progression, and interactions for players.
 */
public final class DefaultQuestEngine implements QuestEngine {

    private final Logger log = ChronaLog.get(DefaultQuestEngine.class);

    private final QuestRegistry registry;
    private final QuestStateStore stateStore;
    private final ConditionEngine conditionEngine;
    private final RewardEngine rewardEngine;
    private final ConditionContextFactory conditionCtxFactory;
    private final RewardContextFactory rewardCtxFactory;
    private final ActionContextFactory actionCtxFactory;
    private final DefaultActionExecutor actionExecutor;

    public DefaultQuestEngine(QuestRegistry registry,
                              QuestStateStore stateStore,
                              ConditionEngine conditionEngine,
                              RewardEngine rewardEngine,
                              ConditionContextFactory conditionCtxFactory,
                              RewardContextFactory rewardCtxFactory,
                              ActionContextFactory actionCtxFactory,
                              DefaultActionExecutor actionExecutor) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.conditionEngine = Objects.requireNonNull(conditionEngine, "conditionEngine");
        this.rewardEngine = Objects.requireNonNull(rewardEngine, "rewardEngine");
        this.conditionCtxFactory = Objects.requireNonNull(conditionCtxFactory, "conditionCtxFactory");
        this.rewardCtxFactory = Objects.requireNonNull(rewardCtxFactory, "rewardCtxFactory");
        this.actionCtxFactory = Objects.requireNonNull(actionCtxFactory, "actionCtxFactory");
        this.actionExecutor = Objects.requireNonNull(actionExecutor, "actionExecutor");
    }

    @Override
    public Optional<QuestDefinition> getDefinition(String questId) {
        return registry.get(questId);
    }

    @Override
    public PlayerQuestState getOrCreateState(UUID playerId, String questId) {
        QuestDefinition def = registry.get(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown questId " + questId));
        PlayerQuestState s = stateStore.getOrCreateState(playerId, def);
        // repeatability comes from definition, not DB
        return new PlayerQuestState(
                s.playerId(),
                s.questId(),
                def.type(),
                def.repeatability(),
                s.state(),
                s.currentObjectiveIndex(),
                s.startedAt(),
                s.completedAt(),
                s.failedAt(),
                s.lastUpdatedAt(),
                s.expiresAt(),
                s.nextAvailableAt(),
                s.timesCompleted(),
                s.timesFailed(),
                s.lastResult()
        );
    }

    @Override
    public boolean canStart(PlayerQuestState state, QuestDefinition def) {
        Instant now = Instant.now();

        if (state.state() == QuestRunState.ACTIVE)
            return false;

        if (state.state() == QuestRunState.COMPLETED &&
                def.repeatability() == QuestRepeatability.ONCE)
            return false;

        if (state.state() == QuestRunState.COOLDOWN &&
                state.nextAvailableAt() != null &&
                state.nextAvailableAt().isAfter(now))
            return false;

        // Quest-Level-Conditions
        ConditionContext ctx = conditionCtxFactory.create(state.playerId(), def);
        return conditionEngine.evaluate(def.requirements().logic(), ctx);
    }

    @Override
    public PlayerQuestState startQuest(UUID playerId, String questId, QuestContext ctx) {
        QuestDefinition def = registry.get(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown questId " + questId));

        PlayerQuestState state = getOrCreateState(playerId, questId);
        QuestRunState oldState = state.state();

        if (!canStart(state, def)) {
            log.debug("Player {} cannot start quest {} (state={}, repeat={})",
                    playerId, questId, state.state(), def.repeatability());
            return state;
        }

        Instant now = Instant.now();
        state.setState(QuestRunState.ACTIVE);
        state.setStartedAt(now);
        state.setCompletedAt(null);
        state.setFailedAt(null);

        // Timing
        if (def.timing() != null && def.timing().expiresAfterSeconds() != null)
            state.setExpiresAt(now.plusSeconds(def.timing().expiresAfterSeconds()));
        else
            state.setExpiresAt(null);

        // Objective-Progress initialisieren
        stateStore.deleteObjectiveProgress(playerId, questId);

        if (!def.objectives().isEmpty()) {
            if (def.flowMode() == QuestFlowMode.SEQUENTIAL)
                state.setCurrentObjectiveIndex(0);
            else
                state.setCurrentObjectiveIndex(null); // PARALLEL

            for (ObjectiveDef obj : def.objectives()) {
                ObjectiveProgress p = new ObjectiveProgress(playerId, questId, obj.id(), 0L, false, Instant.now());
                stateStore.saveObjectiveProgress(p);
            }
        }

        stateStore.saveState(state);

        // Objective-onStart für aktive Objectives ausführen
        if (!def.objectives().isEmpty()) {
            if (def.flowMode() == QuestFlowMode.SEQUENTIAL) {
                ObjectiveDef first = def.objectives().getFirst();
                runObjectiveOnStart(playerId, def, first, ctx);
            }
            else {
                for (ObjectiveDef obj : def.objectives())
                    runObjectiveOnStart(playerId, def, obj, ctx);
            }
        }

        logTransition(playerId, def, oldState, state.state(), "START", ctx);
        return state;
    }

    @Override
    public void abandonQuest(UUID playerId, String questId, QuestContext ctx) {
        QuestDefinition def = registry.get(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown questId " + questId));
        PlayerQuestState state = getOrCreateState(playerId, questId);
        QuestRunState oldState = state.state();

        if (state.state() != QuestRunState.ACTIVE)
            return;

        state.setState(QuestRunState.ABANDONED);
        state.setStartedAt(null);
        state.setCompletedAt(null);
        state.setFailedAt(null);
        state.setExpiresAt(null);
        state.setCurrentObjectiveIndex(null);

        if (def.repeatability() == QuestRepeatability.DAILY ||
                def.repeatability() == QuestRepeatability.WEEKLY ||
                def.repeatability() == QuestRepeatability.EVENT ||
                def.repeatability() == QuestRepeatability.INFINITE) {
            state.setState(QuestRunState.COOLDOWN);
        }

        stateStore.saveState(state);
        stateStore.deleteObjectiveProgress(playerId, questId);

        logTransition(playerId, def, oldState, state.state(), "ABANDON", ctx);
    }

    @Override
    public void failQuest(UUID playerId, String questId, String reason, QuestContext ctx) {
        QuestDefinition def = registry.get(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown questId " + questId));
        PlayerQuestState state = getOrCreateState(playerId, questId);
        QuestRunState oldState = state.state();

        if (state.state() != QuestRunState.ACTIVE)
            return;

        state.setState(QuestRunState.FAILED);
        state.setFailedAt(Instant.now());
        state.incTimesFailed();
        state.setCurrentObjectiveIndex(null);
        state.setExpiresAt(null);

        if (def.repeatability() != QuestRepeatability.ONCE)
            state.setState(QuestRunState.COOLDOWN);

        stateStore.saveState(state);

        QuestContext ctxWithReason = ctx != null
                ? ctx
                : QuestContext.builder().reason(reason).build();

        logTransition(playerId, def, oldState, state.state(), "FAIL", ctxWithReason);
    }

    @Override
    public void completeQuest(UUID playerId, String questId, QuestContext ctx) {
        QuestDefinition def = registry.get(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown questId " + questId));
        PlayerQuestState state = getOrCreateState(playerId, questId);
        QuestRunState oldState = state.state();

        if (state.state() != QuestRunState.ACTIVE)
            return;

        state.setState(QuestRunState.COMPLETED);
        state.setCompletedAt(Instant.now());
        state.incTimesCompleted();
        state.setCurrentObjectiveIndex(null);
        state.setExpiresAt(null);

        if (def.repeatability() != QuestRepeatability.ONCE)
            state.setState(QuestRunState.COOLDOWN);

        stateStore.saveState(state);

        // Rewards anwenden
        RewardDef rewards = def.rewards();
        if (rewards != null) {
            RewardContext rewardCtx = rewardCtxFactory.create(playerId, def);
            ActionExecutionContext actionCtx = actionCtxFactory.create(playerId, def, ctx);
            rewardEngine.applyRewards(rewardCtx, actionCtx, def, rewards);
        }

        logTransition(playerId, def, oldState, state.state(), "COMPLETE", ctx);
    }

    @Override
    public void applyObjectiveProgress(UUID playerId, String questId, String objectiveId,
                                       long increment, QuestContext ctx) {

        QuestDefinition def = registry.get(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown questId " + questId));
        PlayerQuestState state = getOrCreateState(playerId, questId);

        if (state.state() != QuestRunState.ACTIVE)
            return;

        ObjectiveDef obj = def.objectives().stream()
                .filter(o -> o.id().equals(objectiveId))
                .findFirst()
                .orElse(null);
        if (obj == null)
            return;

        // SEQUENTIAL: nur aktuelle Objective nimmt Fortschritt an
        if (def.flowMode() == QuestFlowMode.SEQUENTIAL) {
            int indexOfObj = indexOfObjective(def, obj);
            Integer currentIndex = state.currentObjectiveIndex();
            if (currentIndex == null || indexOfObj != currentIndex)
                return;
        }

        // Objective-spezifische Conditions
        ConditionContext condCtx = conditionCtxFactory.create(playerId, def);
        if (!conditionEngine.evaluate(obj.requirements(), condCtx))
            return;

        ObjectiveProgress progress = stateStore.getOrCreateObjectiveProgress(playerId, questId, objectiveId);
        boolean wasCompleted = progress.isCompleted();

        long target = obj.target() > 0 ? obj.target() : 1;
        long newValue = progress.progress() + increment;

        if (newValue >= target) {
            newValue = target;
            progress.setCompleted(true);
        }

        progress.setProgress(newValue);
        stateStore.saveObjectiveProgress(progress);

        // If objective newly completed → onComplete
        if (!wasCompleted && progress.isCompleted())
            runObjectiveOnComplete(playerId, def, obj, ctx);

        // Flow-Handling
        if (obj.autoComplete() && progress.isCompleted()) {
            if (def.flowMode() == QuestFlowMode.SEQUENTIAL) {
                int idx = state.currentObjectiveIndex() != null ? state.currentObjectiveIndex() : 0;
                int nextIdx = idx + 1;
                if (nextIdx < def.objectives().size()) {
                    state.setCurrentObjectiveIndex(nextIdx);
                    stateStore.saveState(state);

                    ObjectiveDef next = def.objectives().get(nextIdx);
                    runObjectiveOnStart(playerId, def, next, ctx);
                }
                else if (def.autoCompleteOnLastObjective())
                    completeQuest(playerId, questId, ctx);
            }
            else { // PARALLEL
                boolean allDone = def.objectives().stream()
                        .allMatch(o -> {
                            ObjectiveProgress p = stateStore.getOrCreateObjectiveProgress(playerId, questId, o.id());
                            return p.isCompleted();
                        });
                if (allDone && def.autoCompleteOnLastObjective())
                    completeQuest(playerId, questId, ctx);
            }
        }
    }

    @Override
    public List<PlayerQuestState> getActiveQuests(UUID playerId) {
        List<PlayerQuestState> raw = stateStore.getActiveStates(playerId);
        List<PlayerQuestState> out = new ArrayList<>();

        for (PlayerQuestState s : raw) {
            registry.get(s.questId()).ifPresentOrElse(def -> out.add(new PlayerQuestState(
                    s.playerId(),
                    s.questId(),
                    def.type(),
                    def.repeatability(),
                    s.state(),
                    s.currentObjectiveIndex(),
                    s.startedAt(),
                    s.completedAt(),
                    s.failedAt(),
                    s.lastUpdatedAt(),
                    s.expiresAt(),
                    s.nextAvailableAt(),
                    s.timesCompleted(),
                    s.timesFailed(),
                    s.lastResult()
            )), () -> {
                log.warn("getActiveQuests: QuestDefinition {} not found.", s.questId());
                out.add(s);
            });
        }

        return out;
    }

    private int indexOfObjective(QuestDefinition def, ObjectiveDef target) {
        List<ObjectiveDef> list = def.objectives();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(target.id()))
                return i;
        }
        return -1;
    }

    private void runObjectiveOnStart(UUID playerId, QuestDefinition quest, ObjectiveDef obj, QuestContext questCtx) {
        if (obj.onStart().isEmpty())
            return;
        ActionExecutionContext actionCtx = actionCtxFactory.create(playerId, quest, questCtx);
        actionExecutor.executeAll(obj.onStart(), actionCtx);
    }

    private void runObjectiveOnComplete(UUID playerId, QuestDefinition quest, ObjectiveDef obj, QuestContext questCtx) {
        if (obj.onComplete().isEmpty())
            return;

        ActionExecutionContext actionCtx = actionCtxFactory.create(playerId, quest, questCtx);
        actionExecutor.executeAll(obj.onComplete(), actionCtx);
    }

    private void logTransition(UUID playerId,
                               QuestDefinition def,
                               QuestRunState from,
                               QuestRunState to,
                               String action,
                               QuestContext ctx) {

        String world = null;
        Integer x = null, y = null, z = null;
        Map<String, Object> extra = new HashMap<>();

        if (ctx != null) {
            if (ctx.location() != null && ctx.location().getWorld() != null) {
                world = ctx.location().getWorld().getName();
                x = ctx.location().getBlockX();
                y = ctx.location().getBlockY();
                z = ctx.location().getBlockZ();
            }
            if (ctx.npcId() != null)
                extra.put("npcId", ctx.npcId());
            if (ctx.regionId() != null)
                extra.put("regionId", ctx.regionId());
            if (ctx.reason() != null)
                extra.put("reason", ctx.reason());
            extra.putAll(ctx.extra());
        }

        QuestHistoryEntry entry = new QuestHistoryEntry(
                playerId,
                def.id(),
                def.type(),
                action,
                from != null ? from.name() : null,
                to != null ? to.name() : null,
                world,
                x, y, z,
                extra,
                Instant.now()
        );
        stateStore.logHistory(entry);
    }
}
