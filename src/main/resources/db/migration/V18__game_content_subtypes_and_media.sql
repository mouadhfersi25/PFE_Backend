ALTER TABLE questions
ADD COLUMN IF NOT EXISTS sous_type VARCHAR(40);

ALTER TABLE questions
ADD COLUMN IF NOT EXISTS media_url TEXT;

ALTER TABLE questions
ADD COLUMN IF NOT EXISTS prompt_audio_url TEXT;

UPDATE questions
SET sous_type = 'DEFAULT'
WHERE sous_type IS NULL OR TRIM(sous_type) = '';

ALTER TABLE cartes_memoire
ADD COLUMN IF NOT EXISTS card_type VARCHAR(20);

ALTER TABLE cartes_memoire
ADD COLUMN IF NOT EXISTS card_value TEXT;

ALTER TABLE cartes_memoire
ADD COLUMN IF NOT EXISTS sous_type VARCHAR(40);

UPDATE cartes_memoire
SET card_type = 'EMOJI'
WHERE card_type IS NULL OR TRIM(card_type) = '';

UPDATE cartes_memoire
SET card_value = COALESCE(NULLIF(TRIM(card_value), ''), symbole);

UPDATE cartes_memoire
SET sous_type = 'DEFAULT'
WHERE sous_type IS NULL OR TRIM(sous_type) = '';

UPDATE parametres_reflexe
SET modele_reflexe = 'CLASSIC'
WHERE modele_reflexe IS NULL
   OR upper(modele_reflexe) NOT IN ('CLASSIC', 'GO_NO_GO', 'CHOICE_REACTION', 'STROOP_INVERSE');

ALTER TABLE parametres_reflexe
DROP CONSTRAINT IF EXISTS ck_param_reflex_modele_reflexe;

ALTER TABLE parametres_reflexe
ADD CONSTRAINT ck_param_reflex_modele_reflexe
CHECK (modele_reflexe IN ('CLASSIC', 'GO_NO_GO', 'CHOICE_REACTION', 'STROOP_INVERSE'));

UPDATE puzzles_logiques
SET sous_type = 'DEDUCTION'
WHERE sous_type IS NULL OR TRIM(sous_type) = '';

UPDATE puzzles_logiques
SET sous_type = 'DEDUCTION'
WHERE sous_type NOT IN ('SUITE_LOGIQUE', 'INTRUS', 'DEDUCTION', 'COLOR_MATCH');

ALTER TABLE puzzles_logiques
DROP CONSTRAINT IF EXISTS chk_puzzles_logiques_sous_type;

ALTER TABLE puzzles_logiques
ADD CONSTRAINT chk_puzzles_logiques_sous_type
CHECK (sous_type IN ('SUITE_LOGIQUE', 'INTRUS', 'DEDUCTION', 'COLOR_MATCH'));
