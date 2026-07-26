-- V2: economy wallets (DATABASE.md section 4, `wallets`).
-- Balances are integral minor units; never floating point (docs/economy.md).
CREATE TABLE IF NOT EXISTS wallets (
    uuid CHAR(36) NOT NULL,
    balance_minor_units BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (uuid)
);
