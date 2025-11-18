-- V3__player_flags.sql
-- Aktive Flags + Metadaten

CREATE TABLE IF NOT EXISTS player_flag (
    player_id  UUID NOT NULL,
    flag_key   TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source     TEXT,            -- z.B. "dialogue:ARENKHET_INTRO" oder "quest:ARC-PH1-01"
    extra      JSONB,           -- freie Zusatzinfos (npcId, questId, etc.)
    PRIMARY KEY (player_id, flag_key)
);

CREATE INDEX IF NOT EXISTS idx_player_flag_player
    ON player_flag (player_id);

CREATE INDEX IF NOT EXISTS idx_player_flag_key
    ON player_flag (flag_key);


-- History-Log aller Änderungen

CREATE TABLE IF NOT EXISTS player_flag_log (
    id         BIGSERIAL PRIMARY KEY,
    player_id  UUID NOT NULL,
    flag_key   TEXT NOT NULL,
    value      BOOLEAN NOT NULL,   -- true = gesetzt, false = gelöscht
    action     TEXT NOT NULL,      -- "SET" oder "UNSET"
    source     TEXT,
    extra      JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_player_flag_log_player
    ON player_flag_log (player_id);

CREATE INDEX IF NOT EXISTS idx_player_flag_log_flag
    ON player_flag_log (flag_key);
