ALTER TABLE jeux
    ADD COLUMN IF NOT EXISTS quiz_variant VARCHAR(40) NOT NULL DEFAULT 'DEFAULT';

-- Jeux QUIZ existants : déduire la variante depuis les questions (priorité aux variantes non-DEFAULT).
UPDATE jeux j
SET quiz_variant = COALESCE(
    (
        SELECT UPPER(q.sous_type)
        FROM questions q
        WHERE q.id_jeu = j.id
          AND q.sous_type IS NOT NULL
          AND TRIM(q.sous_type) <> ''
          AND UPPER(q.sous_type) <> 'DEFAULT'
        ORDER BY q.id
        LIMIT 1
    ),
    (
        SELECT UPPER(q.sous_type)
        FROM questions q
        WHERE q.id_jeu = j.id
          AND q.sous_type IS NOT NULL
          AND TRIM(q.sous_type) <> ''
        ORDER BY q.id
        LIMIT 1
    ),
    'DEFAULT'
)
WHERE j.type_jeu = 'QUIZ';

ALTER TABLE jeux
    DROP CONSTRAINT IF EXISTS chk_jeux_quiz_variant;

ALTER TABLE jeux
    ADD CONSTRAINT chk_jeux_quiz_variant
        CHECK (quiz_variant IN (
            'DEFAULT', 'TRUE_FALSE', 'CLOZE', 'IMAGE_WORD',
            'SYNONYM_ANTONYM', 'COLOR_TRANSLATION', 'AUDIO_COLOR'
        ));

-- Aligner toutes les questions sur la variante du jeu.
UPDATE questions q
SET sous_type = j.quiz_variant
FROM jeux j
WHERE q.id_jeu = j.id
  AND j.type_jeu = 'QUIZ';

ALTER TABLE questions
    DROP CONSTRAINT IF EXISTS chk_questions_sous_type;

ALTER TABLE questions
    ADD CONSTRAINT chk_questions_sous_type
        CHECK (sous_type IN (
            'DEFAULT', 'TRUE_FALSE', 'CLOZE', 'IMAGE_WORD',
            'SYNONYM_ANTONYM', 'COLOR_TRANSLATION', 'AUDIO_COLOR'
        ));
