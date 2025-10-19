package dev.chrona.economy.infrastructure;

import dev.chrona.common.Db;
import dev.chrona.common.economy.TransactionOrigin;
import dev.chrona.economy.domain.Wallet;
import dev.chrona.economy.domain.WalletRepository;

import java.sql.*; import java.util.*;

public final class JdbcWalletService implements dev.chrona.economy.application.WalletService {

    private final WalletRepository repo;

    public JdbcWalletService(WalletRepository repo){
        this.repo = repo;
    }

    private boolean claimExists(Connection con, UUID claimId) throws SQLException {
        try (var ps = con.prepareStatement("select 1 from econ_claim where claim_id=?")) {
            ps.setObject(1, claimId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    private void insertClaim(Connection con, UUID claimId, UUID pid, long amount, TransactionOrigin origin) throws SQLException {
        try (var ps = con.prepareStatement("""
      insert into econ_claim(claim_id, player_id, amount, source_domain, source_key)
      values (?,?,?,?,?)
    """)) {
            ps.setObject(1, claimId);
            ps.setObject(2, pid);
            ps.setLong(3, amount);
            ps.setString(4, origin.domain().name());
            ps.setString(5, origin.key());
            ps.executeUpdate();
        }
    }

    @Override public void credit(UUID pid, long amt, UUID claimId, TransactionOrigin origin) {
        try (var con = Db.ds().getConnection()) {
            con.setAutoCommit(false);
            if (claimExists(con, claimId)) {
                con.rollback();
                return;
            }

            var w = repo.find(pid).orElseGet(() -> {
                var nw = new Wallet(pid,0,0);
                repo.insert(nw);
                return nw;
            });

            w.credit(amt);
            if (!repo.update(w))
                throw new SQLException("optimistic lock failed");
            insertClaim(con, claimId, pid, +amt, origin);
            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void debit(UUID pid, long amt, UUID claimId, TransactionOrigin origin) {
        try (var con = Db.ds().getConnection()) {
            con.setAutoCommit(false);

            if (claimExists(con, claimId)) {
                con.rollback();
                return;
            }

            var w = repo.find(pid).orElseThrow();
            w.debit(amt);
            if (!repo.update(w))
                throw new SQLException("optimistic lock failed");
            insertClaim(con, claimId, pid, -amt, origin);
            con.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
