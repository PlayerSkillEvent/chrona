package dev.chrona.quest.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.chrona.common.Db;
import dev.chrona.common.log.ChronaLog;
import dev.chrona.quest.model.QuestDefinition;
import dev.chrona.quest.model.QuestRepeatability;
import dev.chrona.quest.model.QuestType;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Database-backed implementation of the QuestStateStore interface.
 * This class handles the persistence and retrieval of player quest states,
 * objective progress, and quest history using a relational database.
 */
public final class DbQuestStateStore implements QuestStateStore {

    private static final Logger log = ChronaLog.get(DbQuestStateStore.class);
    private static final String T_STATE = "player_quest";
    private static final String T_OBJ = "player_quest_objective";
    private static final String T_HIST = "player_quest_history";

    private final DataSource ds;
    private final Gson gson = new GsonBuilder().create();

    public DbQuestStateStore() {
        this.ds = Db.ds();
    }

    @Override
    public Optional<PlayerQuestState> findState(UUID playerId, String questId) {
        String sql = "SELECT * FROM " + T_STATE + " WHERE player_id = ? AND quest_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setString(2, questId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapState(rs));
            }
        }
        catch (SQLException e) {
            log.error("findState({}, {}) failed: {}", playerId, questId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public PlayerQuestState getOrCreateState(UUID playerId, QuestDefinition def) {
        return findState(playerId, def.id())
                .orElseGet(() -> {
                    PlayerQuestState s = new PlayerQuestState(
                            playerId,
                            def.id(),
                            def.type(),
                            def.repeatability(),
                            QuestRunState.LOCKED,
                            null,
                            null,
                            null,
                            null,
                            Instant.now(),
                            null,
                            null,
                            0,
                            0,
                            null
                    );
                    saveState(s);
                    return s;
                });
    }

    @Override
    public void saveState(PlayerQuestState s) {
        String sql = "INSERT INTO " + T_STATE + " (" +
                "player_id, quest_id, type, state, current_obj_index, started_at, completed_at," +
                "failed_at, last_updated_at, expires_at, next_available_at, times_completed," +
                "times_failed, last_result" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (player_id, quest_id) DO UPDATE SET " +
                "type = EXCLUDED.type, " +
                "state = EXCLUDED.state, " +
                "current_obj_index = EXCLUDED.current_obj_index, " +
                "started_at = EXCLUDED.started_at, " +
                "completed_at = EXCLUDED.completed_at, " +
                "failed_at = EXCLUDED.failed_at, " +
                "last_updated_at = EXCLUDED.last_updated_at, " +
                "expires_at = EXCLUDED.expires_at, " +
                "next_available_at = EXCLUDED.next_available_at, " +
                "times_completed = EXCLUDED.times_completed, " +
                "times_failed = EXCLUDED.times_failed, " +
                "last_result = EXCLUDED.last_result";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, s.playerId());
            ps.setString(2, s.questId());
            ps.setString(3, s.type().name());
            ps.setString(4, s.state().name());

            if (s.currentObjectiveIndex() != null)
                ps.setInt(5, s.currentObjectiveIndex());
            else
                ps.setNull(5, Types.INTEGER);

            setInstant(ps, 6, s.startedAt());
            setInstant(ps, 7, s.completedAt());
            setInstant(ps, 8, s.failedAt());
            setInstant(ps, 9, s.lastUpdatedAt());
            setInstant(ps, 10, s.expiresAt());
            setInstant(ps, 11, s.nextAvailableAt());

            ps.setInt(12, s.timesCompleted());
            ps.setInt(13, s.timesFailed());
            if (s.lastResult() != null)
                ps.setString(14, s.lastResult());
            else
                ps.setNull(14, Types.VARCHAR);

            ps.executeUpdate();
        }
        catch (SQLException e) {
            log.error("saveState({}, {}) failed: {}", s.playerId(), s.questId(), e.getMessage(), e);
        }
    }

    @Override
    public List<PlayerQuestState> getStatesByPlayer(UUID playerId) {
        String sql = "SELECT * FROM " + T_STATE + " WHERE player_id = ?";
        List<PlayerQuestState> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapState(rs));
            }
        }
        catch (SQLException e) {
            log.error("getStatesByPlayer({}) failed: {}", playerId, e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PlayerQuestState> getActiveStates(UUID playerId) {
        String sql = "SELECT * FROM " + T_STATE +
                " WHERE player_id = ? AND state = 'ACTIVE'";
        List<PlayerQuestState> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapState(rs));
            }
        }
        catch (SQLException e) {
            log.error("getActiveStates({}) failed: {}", playerId, e.getMessage(), e);
        }
        return list;
    }

    @Override
    public ObjectiveProgress getOrCreateObjectiveProgress(UUID playerId, String questId, String objectiveId) {
        String sql = "SELECT progress_num, completed, last_updated " +
                "FROM " + T_OBJ +
                " WHERE player_id = ? AND quest_id = ? AND objective_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, playerId);
            ps.setString(2, questId);
            ps.setString(3, objectiveId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long progress = rs.getLong("progress_num");
                    boolean completed = rs.getBoolean("completed");
                    Instant lastUpdated = getInstant(rs, "last_updated");
                    return new ObjectiveProgress(playerId, questId, objectiveId, progress, completed, lastUpdated);
                }
            }
        }
        catch (SQLException e) {
            log.error("getOrCreateObjectiveProgress({}, {}, {}) failed: {}",
                    playerId, questId, objectiveId, e.getMessage(), e);
        }

        return new ObjectiveProgress(playerId, questId, objectiveId, 0L, false, Instant.now());
    }



    @Override
    public void saveObjectiveProgress(ObjectiveProgress p) {
        String sql = "INSERT INTO " + T_OBJ +
                " (player_id, quest_id, objective_id, progress_num, completed, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (player_id, quest_id, objective_id) DO UPDATE SET " +
                "progress_num = EXCLUDED.progress_num, " +
                "completed = EXCLUDED.completed, " +
                "last_updated = EXCLUDED.last_updated";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, p.playerId());
            ps.setString(2, p.questId());
            ps.setString(3, p.objectiveId());
            ps.setLong(4, p.progress());
            ps.setBoolean(5, p.isCompleted());
            ps.setTimestamp(6, Timestamp.from(p.lastUpdated()));

            ps.executeUpdate();
        }
        catch (SQLException e) {
            log.error("saveObjectiveProgress({}, {}, {}) failed: {}",
                    p.playerId(), p.questId(), p.objectiveId(), e.getMessage(), e);
        }
    }

    @Override
    public List<ObjectiveProgress> getObjectiveProgress(UUID playerId, String questId) {
        String sql = "SELECT objective_id, progress_num, completed, last_updated " +
                "FROM " + T_OBJ + " WHERE player_id = ? AND quest_id = ?";
        List<ObjectiveProgress> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setString(2, questId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String objectiveId = rs.getString("objective_id");
                    long progress = rs.getLong("progress_num");
                    boolean completed = rs.getBoolean("completed");
                    Instant lastUpdated = getInstant(rs, "last_updated");
                    list.add(new ObjectiveProgress(playerId, questId, objectiveId, progress, completed, lastUpdated));
                }
            }
        }
        catch (SQLException e) {
            log.error("getObjectiveProgress({}, {}) failed: {}", playerId, questId, e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void deleteObjectiveProgress(UUID playerId, String questId) {
        String sql = "DELETE FROM " + T_OBJ + " WHERE player_id = ? AND quest_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setString(2, questId);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            log.error("deleteObjectiveProgress({}, {}) failed: {}", playerId, questId, e.getMessage(), e);
        }
    }

    @Override
    public void logHistory(QuestHistoryEntry e) {
        String sql = "INSERT INTO " + T_HIST +
                " (player_id, quest_id, type, action, from_state, to_state, world, x, y, z, extra, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, e.playerId());
            ps.setString(2, e.questId());
            ps.setString(3, e.type().name());
            ps.setString(4, e.action());
            ps.setString(5, e.fromState());
            ps.setString(6, e.toState());
            ps.setString(7, e.world());
            if (e.x() != null) ps.setInt(8, e.x()); else ps.setNull(8, Types.INTEGER);
            if (e.y() != null) ps.setInt(9, e.y()); else ps.setNull(9, Types.INTEGER);
            if (e.z() != null) ps.setInt(10, e.z()); else ps.setNull(10, Types.INTEGER);
            if (e.extra() != null && !e.extra().isEmpty())
                ps.setString(11, gson.toJson(e.extra()));
            else
                ps.setNull(11, Types.VARCHAR);
            ps.setTimestamp(12, Timestamp.from(e.createdAt()));

            ps.executeUpdate();
        }
        catch (SQLException ex) {
            log.error("logHistory({}, {}) failed: {}", e.playerId(), e.questId(), ex.getMessage(), ex);
        }
    }

    private PlayerQuestState mapState(ResultSet rs) throws SQLException {
        UUID playerId = (UUID) rs.getObject("player_id");
        String questId = rs.getString("quest_id");
        QuestType type = QuestType.valueOf(rs.getString("type"));
        QuestRunState state = QuestRunState.valueOf(rs.getString("state"));

        Integer currentIdx = (Integer) rs.getObject("current_obj_index");
        Instant startedAt = getInstant(rs, "started_at");
        Instant completedAt = getInstant(rs, "completed_at");
        Instant failedAt = getInstant(rs, "failed_at");
        Instant lastUpdated = getInstant(rs, "last_updated_at");
        Instant expiresAt = getInstant(rs, "expires_at");
        Instant nextAvailableAt = getInstant(rs, "next_available_at");

        int timesCompleted = rs.getInt("times_completed");
        int timesFailed = rs.getInt("times_failed");
        String lastResult = rs.getString("last_result");

        QuestRepeatability rep = QuestRepeatability.ONCE;

        return new PlayerQuestState(
                playerId,
                questId,
                type,
                rep,
                state,
                currentIdx,
                startedAt,
                completedAt,
                failedAt,
                lastUpdated,
                expiresAt,
                nextAvailableAt,
                timesCompleted,
                timesFailed,
                lastResult
        );
    }

    private static Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }

    private static void setInstant(PreparedStatement ps, int index, Instant instant) throws SQLException {
        if (instant != null)
            ps.setTimestamp(index, Timestamp.from(instant));
        else
            ps.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
    }
}
