-- V1: player profile baseline (DATABASE.md section 4, `player_profiles`).
CREATE TABLE IF NOT EXISTS player_profiles (
    uuid CHAR(36) NOT NULL,
    first_joined_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    display_name_cache VARCHAR(16),
    PRIMARY KEY (uuid)
);

CREATE INDEX IF NOT EXISTS idx_player_profiles_last_seen_at ON player_profiles (last_seen_at);
