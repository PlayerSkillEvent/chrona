package dev.chrona.launcher;

import dev.chrona.common.Db;
import dev.chrona.economy.domain.Wallet;
import dev.chrona.economy.infrastructure.JdbcWalletRepository;
import java.util.UUID;
import java.sql.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Db.migrate(Main.class.getClassLoader(), "classpath:db/migration");

        var repo = new JdbcWalletRepository();
        var id = UUID.randomUUID();

        try(var con = Db.ds().getConnection(); var ps = con.prepareStatement("insert into player(id,name) values(?,?)")) {
            ps.setObject(1,id);
            ps.setString(2,"Elias");
            ps.executeUpdate();
        }
        repo.insert(new Wallet(id,0,0));

        var w = repo.find(id).orElseThrow();
        w.credit(250);
        repo.update(w);

        System.out.println("✅ Balance = " + repo.find(id).get().balance());
    }
}
