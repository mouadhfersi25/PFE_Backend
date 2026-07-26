ALTER TABLE puzzles_logiques
ADD COLUMN IF NOT EXISTS sous_type VARCHAR(40);

UPDATE puzzles_logiques
SET sous_type = CASE
    WHEN donnees IS NOT NULL AND donnees ~ '"type"\s*:\s*"SUITE_LOGIQUE"' THEN 'SUITE_LOGIQUE'
    WHEN donnees IS NOT NULL AND donnees ~ '"type"\s*:\s*"INTRUS"' THEN 'INTRUS'
    WHEN donnees IS NOT NULL AND donnees ~ '"type"\s*:\s*"DEDUCTION"' THEN 'DEDUCTION'
    ELSE 'DEDUCTION'
END
WHERE sous_type IS NULL OR sous_type = '';

CREATE INDEX IF NOT EXISTS idx_puzzles_logiques_sous_type
ON puzzles_logiques(sous_type);

