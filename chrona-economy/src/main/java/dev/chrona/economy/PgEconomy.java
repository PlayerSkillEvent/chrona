package dev.chrona.economy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import java.sql.*;
import java.util.Objects;

/**
 * Postgres-Implementierung der Economy-Logik.
 * Erwartet die Tabellen: player, wallet, econ_claim, econ_transfer
 * (siehe V1__economy_init.sql).
 * <p>
 * Thread-sicher durch DB-Transaktionen + "SELECT ... FOR UPDATE".
 */
public final class PgEconomy implements EconomyService {

    private final DataSource ds;

    public PgEconomy(DataSource dataSource) {
        this.ds = Objects.requireNonNull(dataSource, "dataSource");
    }

    // ---------- Public API ----------

    @Override
    public long getBalance(UUID playerId) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                ensureWalletExists(c, playerId);
                long bal = currentBalance(c, playerId);
                c.commit();
                return bal;
            }
            catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public long pay(UUID from, UUID to, long amount) throws SQLException {
        if (amount <= 0)
            throw new IllegalArgumentException("amount must be > 0");

        if (from == null || to == null)
            throw new IllegalArgumentException("from/to must not be null");

        if (from.equals(to))
            throw new IllegalArgumentException("cannot pay yourself");

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                ensureWalletExists(c, from);
                ensureWalletExists(c, to);

                long fromBal = lockAndGet(c, from);     // lock sender wallet
                lockAndGet(c, to);                      // lock receiver wallet

                if (fromBal < amount)
                    throw new SQLException("INSUFFICIENT_FUNDS");

                updateWallet(c, from, -amount);         // version++, updated_at
                updateWallet(c, to,   +amount);

                UUID corrId = UUID.randomUUID();
                insertTransfer(c, new Transfer(from, to, amount, Transfer.TransferReason.PLAYER_PAYMENT, corrId));
                insertTransfer(c, new Transfer(from, to, -amount, Transfer.TransferReason.PLAYER_PAYMENT, corrId));

                c.commit();
                return fromBal - amount;
            }
            catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public long claimOnce(UUID playerId, UUID claimId, long amount, String source) throws SQLException {
        if (amount <= 0)
            throw new IllegalArgumentException("amount must be > 0");
        Objects.requireNonNull(claimId, "claimId");

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                ensureWalletExists(c, playerId);

                // 1) Versuch: Claim eintragen (einzigartig via claim_id)
                try (PreparedStatement ps = c.prepareStatement(
                        "insert into econ_claim (claim_id, player_id, source, amount) values (?,?,?,?)")) {
                    ps.setObject(1, claimId);
                    ps.setObject(2, playerId);
                    ps.setString(3, source != null ? source : "UNKNOWN");
                    ps.setLong(4, amount);
                    ps.executeUpdate();
                }
                catch (SQLException ex) {
                    if (isUniqueViolation(ex)) {
                        // Bereits beansprucht: Idempotenz -> aktuellen Stand zurück
                        long bal = currentBalance(c, playerId);
                        c.commit();
                        return bal;
                    }
                    throw ex;
                }

                // 2) Gutschrift
                lockAndGet(c, playerId);
                updateWallet(c, playerId, +amount);
                insertTransfer(c, new Transfer(null, playerId, amount, Transfer.TransferReason.CLAIM, null));

                c.commit();
                return currentBalance(c, playerId);
            }
            catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public long mint(UUID from, UUID to, long amount, UUID corrId) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                ensureWalletExists(c, to);
                lockAndGet(c, to);
                updateWallet(c, to, +amount);
                insertTransfer(c, new Transfer(from, to, amount, Transfer.TransferReason.ADMIN_MINT, corrId));
                c.commit();
                return currentBalance(c, to);
            }
            catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public long burn(UUID sender, UUID from, long amount, UUID corrId) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                ensureWalletExists(c, from);
                long bal = lockAndGet(c, from);
                if (bal < amount) throw new SQLException("INSUFFICIENT_FUNDS");
                updateWallet(c, from, -amount);
                insertTransfer(c, new Transfer(from, null, -amount, Transfer.TransferReason.ADMIN_BURN, corrId));
                c.commit();
                return currentBalance(c, from);
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public Transfer[] getTransfers(UUID playerId, int limit, int offset) throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "select transfer_id, from_player, to_player, amount, reason, corr_id, created_at " +
                            "from econ_transfer " +
                            "where from_player = ? or to_player = ? " +
                            "order by created_at desc " +
                            "limit ? offset ?")) {
                ps.setObject(1, playerId);
                ps.setObject(2, playerId);
                ps.setInt(3, limit);
                ps.setInt(4, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    ArrayList<Transfer> transfers = new ArrayList<>();
                    while (rs.next()) {
                        UUID transferId = (UUID) rs.getObject(1);
                        UUID fromPlayer = (UUID) rs.getObject(2);
                        UUID toPlayer = (UUID) rs.getObject(3);
                        long amount = rs.getLong(4);
                        String reasonStr = rs.getString(5);
                        UUID corrId = (UUID) rs.getObject(6);
                        long timestamp = rs.getTimestamp(7).getTime();

                        Transfer.TransferReason reason = Transfer.TransferReason.valueOf(reasonStr);

                        transfers.add(new Transfer(transferId, fromPlayer, toPlayer, amount, reason, corrId, timestamp));
                    }
                    return transfers.toArray(new Transfer[0]);
                }
            }
        }
    }

    // ---------- Internals ----------

    private static boolean isUniqueViolation(SQLException ex) {
        // Postgres UNIQUE VIOLATION: 23505
        return "23505".equals(ex.getSQLState());
    }

    private static String reasonOrDefault(String reason, String def) {
        return (reason == null || reason.isBlank()) ? def : reason;
    }

    /**
     * Stellt sicher, dass ein Wallet existiert (Upsert leichtgewichtig).
     * Kein Lock hier – Lock passiert in lockAndGet().
     */
    private static void ensureWalletExists(Connection c, UUID playerId) throws SQLException {
        // Versuche direkt insert; bei Konflikt tue nichts.
        try (PreparedStatement ps = c.prepareStatement(
                "insert into wallet (player_id, balance, version) values (?,?,0) " +
                        "on conflict (player_id) do nothing")) {
            ps.setObject(1, playerId);
            ps.setLong(2, 0L);
            ps.executeUpdate();
        }
    }

    /**
     * Liest den aktuellen Kontostand und sperrt die Zeile via FOR UPDATE.
     * Existiert kein Wallet (was nach ensureWalletExists selten ist), wird eine SQLException geworfen.
     */
    private static long lockAndGet(Connection c, UUID playerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select balance from wallet where player_id = ? for update")) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("WALLET_NOT_FOUND");
                return rs.getLong(1);
            }
        }
    }

    /** Liest den Kontostand ohne Lock (für Rückgabe nach Commit okay). */
    private static long currentBalance(Connection c, UUID playerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select balance from wallet where player_id = ?")) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("WALLET_NOT_FOUND");
                return rs.getLong(1);
            }
        }
    }

    /**
     * Addiert delta (+/-) auf das Wallet und erhöht version.
     * Nutzt die aktuelle Version implizit (keine WHERE version = ?), weil wir die Zeile bereits FOR UPDATE gesperrt haben.
     * Wenn du explizites Optimistic Locking willst, erweitere um WHERE version = ? und prüfe die updateCount==1.
     */
    private static void updateWallet(Connection c, UUID playerId, long delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "update wallet set balance = balance + ?, version = version + 1, updated_at = now() where player_id = ?")) {
            ps.setLong(1, delta);
            ps.setObject(2, playerId);
            int n = ps.executeUpdate();
            if (n != 1) throw new SQLException("WALLET_UPDATE_FAILED");
        }
    }

    private static void insertTransfer(Connection c, Transfer transfer) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into econ_transfer (transfer_id, from_player, to_player, amount, reason, corr_id) " +
                        "values (?, ?, ?, ?, ?, ?)"))
        {
            ps.setObject(1, UUID.randomUUID());

            UUID from = transfer.from();
            if (from == null)
                ps.setNull(2, Types.OTHER); else ps.setObject(2, from);

            UUID to = transfer.to();
            if (to == null)
                ps.setNull(3, Types.OTHER); else ps.setObject(3, to);

            ps.setLong(4, transfer.amount());
            ps.setString(5, transfer.reason().value());

            if (transfer.corrId() == null)
                ps.setNull(6, Types.OTHER); else ps.setObject(6, transfer.corrId());

            ps.executeUpdate();
        }
    }


}

