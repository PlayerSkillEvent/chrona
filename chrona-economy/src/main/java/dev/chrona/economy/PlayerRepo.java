package dev.chrona.economy;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

public final class PlayerRepo {
    private final DataSource ds;

    public PlayerRepo(DataSource ds) {
        this.ds = ds;
    }

    /** Legt player + wallet an (idempotent) und aktualisiert Name/Locale/last_seen. */
    public void ensurePlayerAndWallet(UUID playerId, String name, String locale) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                // player upsert
                try (PreparedStatement ps = c.prepareStatement("""
                    insert into player (id, name, locale, first_join, last_seen, version, updated_at)
                    values (?, ?, ?, now(), now(), 0, now())
                    on conflict (id) do update
                    set name = excluded.name,
                        locale = excluded.locale,
                        last_seen = now(),
                        updated_at = now()
                """)) {
                    ps.setObject(1, playerId);
                    ps.setString(2, name);
                    ps.setString(3, (locale == null || locale.isBlank()) ? "en" : locale);
                    ps.executeUpdate();
                }

                // wallet ensure (idempotent)
                try (PreparedStatement ps = c.prepareStatement("""
                    insert into wallet (player_id, balance, version, updated_at)
                    values (?, 0, 0, now())
                    on conflict (player_id) do nothing
                """)) {
                    ps.setObject(1, playerId);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Optional: nur last_seen aktualisieren (z.B. bei Quit). */
    public void touchLastSeen(UUID playerId) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                update player set last_seen = now(), updated_at = now() where id = ?
             """)) {
            ps.setObject(1, playerId);
            ps.executeUpdate();
        }
    }
}

