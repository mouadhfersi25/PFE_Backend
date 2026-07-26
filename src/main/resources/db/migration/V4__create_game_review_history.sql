CREATE TABLE IF NOT EXISTS game_review_history (
    id BIGSERIAL PRIMARY KEY,
    jeu_id BIGINT NOT NULL,
    admin_id BIGINT,
    action VARCHAR(20) NOT NULL,
    motif_refus TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_game_review_history_jeu_id ON game_review_history (jeu_id);
CREATE INDEX IF NOT EXISTS idx_game_review_history_admin_id ON game_review_history (admin_id);
CREATE INDEX IF NOT EXISTS idx_game_review_history_created_at ON game_review_history (created_at);

ALTER TABLE game_review_history
    ADD CONSTRAINT fk_game_review_history_jeu
        FOREIGN KEY (jeu_id) REFERENCES jeux(id) ON DELETE CASCADE;

ALTER TABLE game_review_history
    ADD CONSTRAINT fk_game_review_history_admin
        FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL;
