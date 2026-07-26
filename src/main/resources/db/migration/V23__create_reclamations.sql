CREATE TABLE IF NOT EXISTS reclamations (
    id BIGSERIAL PRIMARY KEY,
    id_utilisateur BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id_jeu BIGINT NOT NULL REFERENCES jeux(id) ON DELETE CASCADE,
    id_session BIGINT NOT NULL REFERENCES sessions_jeu(id) ON DELETE CASCADE,
    motif VARCHAR(50) NOT NULL,
    commentaire TEXT,
    reponse_admin TEXT,
    id_admin BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reclamations_session ON reclamations(id_session);
CREATE INDEX IF NOT EXISTS idx_reclamations_created_at ON reclamations(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reclamations_pending ON reclamations(id_admin) WHERE id_admin IS NULL;
