package dev.chrona.job.core;

import dev.chrona.economy.EconomyService;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class JobRewardRuntime implements JobRuntime {
    private final EconomyService econ;
    private final DataSource ds;
    private final Supplier<String> season;

    public JobRewardRuntime(EconomyService econ, DataSource ds, Supplier<String> season) {
        this.econ = econ; this.ds = ds; this.season = season;
    }

    @Override
    public void reward(UUID playerId, String jobId, long amount, Map<String,Object> payload) {
        UUID jobRunId = UUID.randomUUID();
        // persist job_run (optional analytics)
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
               insert into job_run (id, player_id, job_id, season, payload)
               values (?,?,?,?, to_json(?::text))
             """)) {
            ps.setObject(1, jobRunId);
            ps.setObject(2, playerId);
            ps.setString(3, jobId);
            ps.setString(4, season.get());
            ps.setString(5, payload == null ? "{}" : payload.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            // nicht fatal für Auszahlung
        }
        // deterministische claimId: run + player
        UUID claimId = UUID.nameUUIDFromBytes((jobRunId.toString() + playerId).getBytes(StandardCharsets.UTF_8));
        try {
            long newBalance = econ.claimOnce(playerId, claimId, amount, "JOB:"+jobId+":"+season.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override public Optional<JobPlayerState> getState(UUID playerId, String jobId) {
        // V1 Stub: always level 1
        return Optional.of(new JobPlayerState(1, 0));
    }
}
