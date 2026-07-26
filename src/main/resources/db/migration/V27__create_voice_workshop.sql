CREATE TABLE IF NOT EXISTS voice_series (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    langue VARCHAR(10) NOT NULL DEFAULT 'fr',
    difficulte INTEGER,
    etat VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    id_educateur BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,
    CONSTRAINT ck_voice_series_etat CHECK (etat IN ('BROUILLON', 'PUBLIE', 'ARCHIVE'))
);

CREATE TABLE IF NOT EXISTS voice_prompts (
    id BIGSERIAL PRIMARY KEY,
    series_id BIGINT NOT NULL REFERENCES voice_series(id) ON DELETE CASCADE,
    ordre INTEGER NOT NULL DEFAULT 0,
    texte_reference TEXT NOT NULL,
    sous_type VARCHAR(30) NOT NULL DEFAULT 'READ_ALOUD',
    tolerance VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    indice TEXT,
    duree_max_secondes INTEGER NOT NULL DEFAULT 30,
    CONSTRAINT ck_voice_prompt_sous_type CHECK (sous_type IN ('READ_ALOUD', 'REPEAT_AFTER')),
    CONSTRAINT ck_voice_prompt_tolerance CHECK (tolerance IN ('STRICT', 'NORMAL', 'SOUPLE'))
);

CREATE TABLE IF NOT EXISTS sessions_oral (
    id BIGSERIAL PRIMARY KEY,
    id_utilisateur BIGINT NOT NULL REFERENCES users(id),
    id_series BIGINT NOT NULL REFERENCES voice_series(id),
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP,
    duration_seconds INTEGER,
    score_base INTEGER,
    score_final INTEGER,
    xp_gained INTEGER,
    accuracy_percent INTEGER,
    prompts_total INTEGER,
    prompts_reussis INTEGER,
    niveau_atteint INTEGER,
    scoring_rules_version VARCHAR(32),
    etat_session VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    CONSTRAINT ck_sessions_oral_etat CHECK (etat_session IN ('EN_COURS', 'TERMINE', 'ABANDON'))
);

CREATE TABLE IF NOT EXISTS voice_attempts (
    id BIGSERIAL PRIMARY KEY,
    prompt_id BIGINT NOT NULL REFERENCES voice_prompts(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    session_oral_id BIGINT REFERENCES sessions_oral(id) ON DELETE SET NULL,
    transcription TEXT,
    score_contenu INTEGER,
    feedback_json TEXT,
    duree_secondes INTEGER,
    reussite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voice_series_educateur ON voice_series(id_educateur);
CREATE INDEX IF NOT EXISTS idx_voice_series_etat ON voice_series(etat);
CREATE INDEX IF NOT EXISTS idx_voice_prompts_series ON voice_prompts(series_id);
CREATE INDEX IF NOT EXISTS idx_sessions_oral_user ON sessions_oral(id_utilisateur);
CREATE INDEX IF NOT EXISTS idx_voice_attempts_session ON voice_attempts(session_oral_id);
