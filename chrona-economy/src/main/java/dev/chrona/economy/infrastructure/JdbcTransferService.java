package dev.chrona.economy.infrastructure;

import dev.chrona.common.Db;
import dev.chrona.common.economy.TransactionOrigin;
import dev.chrona.economy.application.TransferService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public final class JdbcTransferService implements TransferService {

    @Override
    public void transfer(UUID from, UUID to, long amount, UUID transferId, TransactionOrigin origin) {
        if (amount <= 0)
            throw new IllegalArgumentException("amount must be > 0");
        if (from.equals(to))
            throw new IllegalArgumentException("cannot transfer to self");

        try (var con = Db.ds().getConnection()) {
            con.setAutoCommit(false);
            try {
                if (existsTransfer(con, transferId)) {
                    con.rollback();
                    return;
                }

                ensureWallet(con, from);
                ensureWallet(con, to);

                var a = from.compareTo(to) < 0 ? from : to;
                var b = from.compareTo(to) < 0 ? to : from;

                lockWallet(con, a);
                lockWallet(con, b);

                long fromBal = getBalance(con, from);
                long toBal = getBalance(con, to);

                if (fromBal < amount)
                    throw new SQLException("insufficient funds");

                updateBalance(con, from, fromBal - amount);
                updateBalance(con, to, toBal + amount);

                insertTransfer(con, transferId, from, to, amount, origin);

                insertClaim(con, UUID.randomUUID(), from, -amount, "transfer:"+transferId);
                insertClaim(con, UUID.randomUUID(), to,   +amount, "transfer:"+transferId);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean existsTransfer(Connection con, UUID transferId) throws SQLException {
        try (var ps = con.prepareStatement("select 1 from econ_transfer where transfer_id=?")) {
            ps.setObject(1, transferId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void ensureWallet(Connection con, UUID pid) throws SQLException {
        try (var ps = con.prepareStatement("""
                  insert into wallet(player_id, balance, version)
                  values (?, 0, 0)
                  on conflict (player_id) do nothing
                """)) {
            ps.setObject(1, pid);
            ps.executeUpdate();
        }
    }

    private static void lockWallet(Connection con, UUID pid) throws SQLException {
        try (var ps = con.prepareStatement("select player_id from wallet where player_id=? for update")) {
            ps.setObject(1, pid);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("wallet missing for " + pid);
            }
        }
    }

    private static long getBalance(Connection con, UUID pid) throws SQLException {
        try (var ps = con.prepareStatement("select balance from wallet where player_id=?")) {
            ps.setObject(1, pid);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("wallet missing for " + pid);
                return rs.getLong(1);
            }
        }
    }

    private static void updateBalance(Connection con, UUID pid, long newBal) throws SQLException {
        try (var ps = con.prepareStatement("""
                  update wallet
                     set balance=?, version=version+1, updated_at=now()
                   where player_id=?
                """)) {
            ps.setLong(1, newBal);
            ps.setObject(2, pid);
            if (ps.executeUpdate() != 1) throw new SQLException("wallet update failed " + pid);
        }
    }

    private static void insertTransfer(Connection con, UUID tid, UUID from, UUID to, long amount, TransactionOrigin origin) throws SQLException {
        try (var ps = con.prepareStatement("""
    insert into econ_transfer(transfer_id, from_player, to_player, amount, source_domain, source_key)
    values (?,?,?,?,?,?)
  """)) {
            ps.setObject(1, tid);
            ps.setObject(2, from);
            ps.setObject(3, to);
            ps.setLong(4, amount);
            ps.setString(5, origin.domain().name());
            ps.setString(6, origin.key());
            ps.executeUpdate();
        }
    }

    private static void insertClaim(Connection con, UUID claimId, UUID pid, long amount, String src) throws SQLException {
        try (var ps = con.prepareStatement("insert into econ_claim(claim_id,player_id,amount,source) values (?,?,?,?)")) {
            ps.setObject(1, claimId);
            ps.setObject(2, pid);
            ps.setLong(3, amount);
            ps.setString(4, src);
            ps.executeUpdate();
        }
    }
}
