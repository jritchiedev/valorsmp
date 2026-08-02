-- V2: per-season Valor scores (DATABASE.md section 4, `valor_scores`).
CREATE TABLE IF NOT EXISTS valor_scores (
    uuid CHAR(36) NOT NULL,
    season INT NOT NULL,
    score BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (uuid, season)
);

CREATE INDEX IF NOT EXISTS idx_valor_scores_season_score ON valor_scores (season, score);
