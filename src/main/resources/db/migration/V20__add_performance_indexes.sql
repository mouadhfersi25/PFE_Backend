-- Performance indexes for high-frequency queries and dashboards.
-- IF NOT EXISTS keeps migration idempotent across environments.

CREATE INDEX IF NOT EXISTS idx_sessions_user_state_start
    ON sessions_jeu (id_utilisateur, etat_session, date_debut DESC);

CREATE INDEX IF NOT EXISTS idx_sessions_room_code
    ON sessions_jeu (room_code);

CREATE INDEX IF NOT EXISTS idx_sessions_mode_jeu_lance
    ON sessions_jeu (mode_jeu_lance);

CREATE INDEX IF NOT EXISTS idx_users_email
    ON users (email);

CREATE INDEX IF NOT EXISTS idx_users_id_parent
    ON users (id_parent);
