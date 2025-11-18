package dev.chrona.common.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.chrona.common.Db;
import dev.chrona.common.log.ChronaLog;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * DB-basierter Store für Spieler-Flags mit History-Logging.
 *
 * - Aktive Flags in Tabelle "player_flag"
 * - Jede Änderung wird zusätzlich in "player_flag_log" geschrieben.
 * - One-Time-Migration aus der alten JSON-Datei "player_flags.json", falls vorhanden.
 *
 * API:
 *   - hasFlag(UUID, String)
 *   - setFlag(UUID, String, boolean)
 *   - setFlag(UUID, String, boolean, FlagMetadata)
 *   - getFlags(UUID)
 *   - save() -> NO-OP (für Legacy-Kompatibilität)
 */
public final class PlayerFlagStore {

    private static final String TABLE_FLAGS = "player_flag";
    private static final String TABLE_FLAGS_LOG = "player_flag_log";

    private final Logger log = ChronaLog.get(PlayerFlagStore.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path legacyFile;
    private final DataSource ds;

    public PlayerFlagStore(Path baseDir) {
        this.legacyFile = baseDir.resolve("player_flags.json");
        this.ds = Db.ds();

        // Flyway sollte V3__player_flags.sql bereits ausgeführt haben.
        migrateLegacyIfNeeded();
    }

    /**
     * Prüft, ob noch die alte JSON-Datei existiert und die DB-Tabelle leer ist.
     * Falls ja, Migration der Flags in die DB, dann Umbenennen in .bak.
     */
    private void migrateLegacyIfNeeded() {
        if (!Files.exists(legacyFile)) {
            return;
        }

        log.info("Legacy player_flags.json gefunden unter {} – prüfe, ob Migration nötig ist …", legacyFile);

        try (Connection conn = ds.getConnection()) {
            if (tableHasAnyRows(conn)) {
                log.info("Tabelle {} enthält bereits Daten – Legacy-Migration wird übersprungen.", TABLE_FLAGS);
                return;
            }

            Map<UUID, Set<String>> legacyFlags = readLegacyJson(legacyFile);
            if (legacyFlags.isEmpty()) {
                log.info("Legacy player_flags.json ist leer – keine Migration erforderlich.");
                backupLegacyFile();
                return;
            }

            conn.setAutoCommit(false);

            String sql = "INSERT INTO " + TABLE_FLAGS + " (player_id, flag_key) " +
                    "VALUES (?, ?) ON CONFLICT DO NOTHING";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int count = 0;
                for (var entry : legacyFlags.entrySet()) {
                    UUID playerId = entry.getKey();
                    for (String flag : entry.getValue()) {
                        ps.setObject(1, playerId);
                        ps.setString(2, flag);
                        ps.addBatch();
                        count++;
                    }
                }

                if (count > 0) {
                    ps.executeBatch();
                }

                conn.commit();
                log.info("Legacy-Migration abgeschlossen: {} Flags aus {} in Tabelle {} übernommen.",
                        count, legacyFile, TABLE_FLAGS);
            } catch (Exception e) {
                conn.rollback();
                log.error("Fehler bei Legacy-Migration von {}: {}", legacyFile, e.getMessage(), e);
                return;
            }

            backupLegacyFile();

        } catch (Exception e) {
            log.error("Fehler beim Prüfen/Migrieren der Legacy-Flags: {}", e.getMessage(), e);
        }
    }

    private boolean tableHasAnyRows(Connection conn) {
        String sql = "SELECT 1 FROM " + TABLE_FLAGS + " LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            log.error("Fehler beim Prüfen, ob Tabelle {} Daten enthält: {}", TABLE_FLAGS, e.getMessage(), e);
            return false;
        }
    }

    private Map<UUID, Set<String>> readLegacyJson(Path file) {
        Map<UUID, Set<String>> result = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            var type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> raw = gson.fromJson(reader, type);
            if (raw != null) {
                raw.forEach((k, v) -> {
                    try {
                        UUID id = UUID.fromString(k);
                        result.put(id, new HashSet<>(v));
                    } catch (IllegalArgumentException ex) {
                        log.warn("Ungültige UUID '{}' in legacy player_flags.json – wird ignoriert.", k);
                    }
                });
            }
            log.info("Legacy-Flags gelesen: {} Spieler, Datei {}", result.size(), file);
        } catch (Exception e) {
            log.error("Konnte legacy player_flags.json nicht lesen: {}", e.getMessage(), e);
        }

        return result;
    }

    private void backupLegacyFile() {
        try {
            Path backup = legacyFile.resolveSibling("player_flags.json.bak-" + Instant.now().toEpochMilli());
            Files.move(legacyFile, backup, StandardCopyOption.REPLACE_EXISTING);
            log.info("Legacy player_flags.json nach {} verschoben.", backup);
        } catch (IOException e) {
            log.warn("Konnte legacy player_flags.json nicht als Backup verschieben: {}", e.getMessage());
        }
    }

    /**
     * Liefert true, wenn der Spieler das Flag gesetzt hat.
     */
    public synchronized boolean hasFlag(UUID playerId, String key) {
        String sql = "SELECT 1 FROM " + TABLE_FLAGS + " WHERE player_id = ? AND flag_key = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, playerId);
            ps.setString(2, key);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            log.error("Fehler beim Prüfen von Flag '{}' für Spieler {}: {}", key, playerId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Setzt oder entfernt ein Flag für einen Spieler ohne zusätzliche Metadaten.
     *
     * value == true  -> Flag wird gesetzt
     * value == false -> Flag wird gelöscht
     */
    public synchronized void setFlag(UUID playerId, String key, boolean value) {
        setFlag(playerId, key, value, null);
    }

    /**
     * Setzt oder entfernt ein Flag für einen Spieler mit Metadaten.
     *
     * - Schreibt in player_flag (Upsert / Delete)
     * - Schreibt in player_flag_log (History)
     */
    public synchronized void setFlag(UUID playerId, String key, boolean value, FlagMetadata metadata) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);

            if (value) {
                upsertFlag(conn, playerId, key, metadata);
                insertLog(conn, playerId, key, true, "SET", metadata);
            } else {
                deleteFlag(conn, playerId, key);
                insertLog(conn, playerId, key, false, "UNSET", metadata);
            }

            conn.commit();
        } catch (SQLException e) {
            log.error("Fehler beim Setzen/Löschen von Flag '{}' für Spieler {}: {}",
                    key, playerId, e.getMessage(), e);
        }
    }

    /**
     * Optionales Helper-API, falls du irgendwann alle Flags eines Spielers brauchst.
     */
    public synchronized Set<String> getFlags(UUID playerId) {
        String sql = "SELECT flag_key FROM " + TABLE_FLAGS + " WHERE player_id = ?";
        Set<String> result = new HashSet<>();

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, playerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            log.error("Fehler beim Lesen aller Flags für Spieler {}: {}", playerId, e.getMessage(), e);
        }

        return result;
    }

    private void upsertFlag(Connection conn, UUID playerId, String key, FlagMetadata metadata) throws SQLException {
        String sql = "INSERT INTO " + TABLE_FLAGS + " (player_id, flag_key, created_at, updated_at, source, extra) " +
                "VALUES (?, ?, now(), now(), ?, ?::jsonb) " +
                "ON CONFLICT (player_id, flag_key) DO UPDATE SET " +
                "  updated_at = EXCLUDED.updated_at, " +
                "  source     = EXCLUDED.source, " +
                "  extra      = EXCLUDED.extra";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setString(2, key);

            String source = metadata != null ? metadata.getSource() : null;
            String extraJson = metadata != null && !metadata.getExtra().isEmpty()
                    ? gson.toJson(metadata.getExtra())
                    : null;

            ps.setString(3, source);

            if (extraJson != null) {
                ps.setString(4, extraJson);
            } else {
                ps.setNull(4, Types.VARCHAR);
            }

            ps.executeUpdate();
        }
    }

    private void deleteFlag(Connection conn, UUID playerId, String key) throws SQLException {
        String sql = "DELETE FROM " + TABLE_FLAGS + " WHERE player_id = ? AND flag_key = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setString(2, key);
            ps.executeUpdate();
        }
    }

    private void insertLog(Connection conn, UUID playerId, String key,
                           boolean value, String action, FlagMetadata metadata) throws SQLException {

        String sql = "INSERT INTO " + TABLE_FLAGS_LOG +
                " (player_id, flag_key, value, action, source, extra, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?::jsonb, now())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setString(2, key);
            ps.setBoolean(3, value);
            ps.setString(4, action);

            String source = metadata != null ? metadata.getSource() : null;
            String extraJson = metadata != null && !metadata.getExtra().isEmpty()
                    ? gson.toJson(metadata.getExtra())
                    : null;

            ps.setString(5, source);

            if (extraJson != null) {
                ps.setString(6, extraJson);
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            ps.executeUpdate();
        }
    }
}
