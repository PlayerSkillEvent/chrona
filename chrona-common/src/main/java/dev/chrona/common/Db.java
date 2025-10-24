package dev.chrona.common;

import com.zaxxer.hikari.*;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public final class Db {

    private static HikariDataSource DS;

    public static DataSource ds() {
        if (DS == null) {
            var c = new HikariConfig();

            c.setJdbcUrl(Env.get("DB_URL","jdbc:postgresql://localhost:5432/chrona"));
            c.setUsername(Env.get("DB_USER","chrona_app"));
            c.setPassword(Env.get("DB_PASS","hallo123"));
            c.setDriverClassName("org.postgresql.Driver");

            c.setMaximumPoolSize(8);
            DS = new HikariDataSource(c);
        }

        return DS;
    }

    public static void close() {
        if (DS != null) {
            DS.close();
            DS = null;
        }
    }

    public static void migrate(ClassLoader cl, String... locs) {
        Flyway flyway = Flyway.configure(cl)
                .dataSource(ds())
                .locations(locs.length == 0 ? new String[]{"classpath:db/migration"} : locs)
                .baselineOnMigrate(true)
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .load();

        flyway.migrate();
    }
}
