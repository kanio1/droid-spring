-- baseline schema for BSS backend
CREATE TABLE IF NOT EXISTS flyway_baseline (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
