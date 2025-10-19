package dev.chrona.economy.infrastructure;

import dev.chrona.common.Db;
import dev.chrona.economy.domain.*;
import java.sql.*;
import java.util.*;

public final class JdbcWalletRepository implements WalletRepository {

    @Override
    public Optional<Wallet> find(UUID id) {
        String sql="select balance,version from wallet where player_id=?";

        try(var con = Db.ds().getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setObject(1,id);
            try(var rs=ps.executeQuery()) {
                if(rs.next())
                    return Optional.of(new Wallet(id, rs.getLong(1), rs.getInt(2)));
            }

            return Optional.empty();
        }
        catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insert(Wallet w) {
        String sql="insert into wallet(player_id,balance,version) values(?,?,?)";

        try(var con = Db.ds().getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setObject(1,w.playerId());
            ps.setLong(2,w.balance());
            ps.setInt(3,w.version());
            ps.executeUpdate();
        }
        catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean update(Wallet w) {
        String sql="update wallet set balance=?, version=version+1, updated_at=now() where player_id=? and version=?";

        try(var con=Db.ds().getConnection(); var ps=con.prepareStatement(sql)) {
            ps.setLong(1,w.balance());
            ps.setObject(2,w.playerId());
            ps.setInt(3,w.version());
            int n = ps.executeUpdate();

            if(n==1) {
                w.bump();
                return true;
            }

            return false;
        }
        catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
