ALTER TABLE sessions_jeu
    ADD COLUMN IF NOT EXISTS mode_jeu_lance VARCHAR(20),
    ADD COLUMN IF NOT EXISTS room_code VARCHAR(16),
    ADD COLUMN IF NOT EXISTS team_name VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_sessions_jeu_room_code ON sessions_jeu(room_code);
CREATE INDEX IF NOT EXISTS idx_sessions_jeu_mode_jeu_lance ON sessions_jeu(mode_jeu_lance);
