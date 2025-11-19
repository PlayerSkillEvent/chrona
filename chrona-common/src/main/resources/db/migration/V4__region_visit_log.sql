CREATE TABLE IF NOT EXISTS player_region_log (
    id         BIGSERIAL PRIMARY KEY,
    player_id  UUID NOT NULL,
    region_id  TEXT NOT NULL,
    action     TEXT NOT NULL,
    world      TEXT NOT NULL,
    x          INT,
    y          INT,
    z          INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_player_region_log_player
    ON player_region_log (player_id);

CREATE INDEX IF NOT EXISTS idx_player_region_log_region
    ON player_region_log (region_id);

CREATE INDEX IF NOT EXISTS idx_player_region_log_action_created
    ON player_region_log (action, created_at);
