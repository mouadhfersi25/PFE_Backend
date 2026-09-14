-- Publicités sponsor (stockage local) liées à un ou plusieurs jeux.

CREATE TABLE IF NOT EXISTS publicites (
    id BIGSERIAL PRIMARY KEY,
    contenu VARCHAR(500) NOT NULL,
    video_url TEXT NOT NULL,
    type_publicite VARCHAR(50) NOT NULL DEFAULT 'VIDEO',
    ad_duration_seconds INTEGER NOT NULL DEFAULT 8,
    cta_label VARCHAR(120),
    cta_url TEXT,
    budget_utilise DOUBLE PRECISION NOT NULL DEFAULT 0,
    nb_vues INTEGER NOT NULL DEFAULT 0,
    nb_clics INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    id_createur BIGINT NOT NULL REFERENCES users(id),
    date_creation TIMESTAMP NOT NULL DEFAULT NOW(),
    date_maj TIMESTAMP,
    CONSTRAINT publicites_duration_check CHECK (ad_duration_seconds BETWEEN 3 AND 60),
    CONSTRAINT publicites_type_check CHECK (type_publicite = 'VIDEO')
);

CREATE TABLE IF NOT EXISTS publicite_jeux (
    publicite_id BIGINT NOT NULL REFERENCES publicites(id) ON DELETE CASCADE,
    jeu_id BIGINT NOT NULL REFERENCES jeux(id) ON DELETE CASCADE,
    PRIMARY KEY (publicite_id, jeu_id)
);

CREATE TABLE IF NOT EXISTS interactions_publicite (
    id BIGSERIAL PRIMARY KEY,
    publicite_id BIGINT NOT NULL REFERENCES publicites(id) ON DELETE CASCADE,
    utilisateur_id BIGINT NOT NULL REFERENCES users(id),
    session_id BIGINT,
    type_interaction VARCHAR(20) NOT NULL,
    date_interaction TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT interactions_publicite_type_check CHECK (type_interaction IN ('VIEW', 'CLICK'))
);

CREATE INDEX IF NOT EXISTS idx_publicites_createur ON publicites(id_createur);
CREATE INDEX IF NOT EXISTS idx_publicites_active ON publicites(active);
CREATE INDEX IF NOT EXISTS idx_publicite_jeux_jeu ON publicite_jeux(jeu_id);
CREATE INDEX IF NOT EXISTS idx_interactions_publicite_pub ON interactions_publicite(publicite_id);
